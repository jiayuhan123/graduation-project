from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any, Literal

from langchain.tools import ToolRuntime, tool
from langchain_core.tools import BaseTool
from pydantic import BaseModel, ConfigDict, Field

from app.clients import JavaToolClient
from app.models import RunRequest
from app.observability import AgentMetrics
from app.rag import KnowledgeService
from app.storage import Database

LOCAL_TOOLS = ("retrieve_teaching_knowledge",)


class StrictArguments(BaseModel):
    model_config = ConfigDict(extra="forbid")


class RetrieveKnowledgeArguments(StrictArguments):
    query: str = Field(min_length=2, max_length=500)
    topK: int | None = Field(default=None, ge=1, le=12)
    metadataFilter: dict[str, Any] | None = None


class ClassDashboardArguments(StrictArguments):
    classId: int
    days: int = Field(default=30, ge=7, le=90)


class SearchQuestionsArguments(StrictArguments):
    keyword: str | None = None
    knowledgePointId: int | None = None
    questionType: Literal["ARITHMETIC", "FILL_BLANK", "VERTICAL", "CHOICE", "JUDGEMENT"] | None = (
        None
    )
    difficulty: int | None = Field(default=None, ge=1, le=5)
    size: int = Field(default=10, ge=1, le=30)


class AssignmentQuestion(StrictArguments):
    questionId: int
    layerCode: str
    sequenceNo: int = Field(ge=1)
    score: float = Field(gt=0)


class AssignmentDraftArguments(StrictArguments):
    classId: int
    title: str = Field(max_length=128)
    description: str | None = Field(default=None, max_length=1000)
    assignmentType: Literal["UNIFIED", "LAYERED"]
    startAt: str
    dueAt: str
    settings: str | None = None
    questions: list[AssignmentQuestion] = Field(min_length=1, max_length=100)


@dataclass(slots=True)
class AgentContext:
    request: RunRequest
    java_tools: JavaToolClient
    knowledge: KnowledgeService
    database: Database
    metrics: AgentMetrics
    used_tools: list[str] = field(default_factory=list)
    tool_call_count: int = 0
    rag_hit_count: int = 0


async def _invoke_java(
    context: AgentContext,
    runtime: ToolRuntime[AgentContext],
    tool_name: str,
    arguments: dict[str, Any],
) -> dict[str, Any]:
    if tool_name not in context.request.actor.allowedTools:
        raise RuntimeError(f"模型请求了未授权工具：{tool_name}")
    prerequisites = {"get_class_dashboard", "search_questions"}
    if tool_name == "create_assignment_draft" and not prerequisites.issubset(context.used_tools):
        missing = ",".join(sorted(prerequisites.difference(context.used_tools)))
        raise RuntimeError(f"生成作业草稿前必须先读取学情并检索固定题库；仍缺少：{missing}")

    runtime.stream_writer({"event": "tool_start", "toolName": tool_name, "arguments": arguments})
    started = time.monotonic()
    status = "SUCCEEDED"
    result: Any = None
    try:
        result = await context.java_tools.invoke(context.request, tool_name, arguments)
        context.used_tools.append(tool_name)
        return result if isinstance(result, dict) else {"value": result}
    except Exception as exception:
        status = "FAILED"
        result = {"error": str(exception)}
        raise
    finally:
        duration_ms = int((time.monotonic() - started) * 1000)
        context.tool_call_count += 1
        await context.database.record_tool(
            context.request.runId, tool_name, arguments, result, status, duration_ms
        )
        context.metrics.tool_calls.labels(tool=tool_name, status=status).inc()
        runtime.stream_writer(
            {
                "event": "tool_result",
                "toolName": tool_name,
                "status": status,
                "durationMs": duration_ms,
                "result": result,
            }
        )


@tool(
    "retrieve_teaching_knowledge",
    args_schema=RetrieveKnowledgeArguments,
    description="从已入库教材、教案和教学规范中执行 Agentic RAG，返回可追溯引用。",
)
async def retrieve_teaching_knowledge(
    query: str,
    runtime: ToolRuntime[AgentContext],
    topK: int | None = None,
    metadataFilter: dict[str, Any] | None = None,
) -> dict[str, Any]:
    context = runtime.context
    tool_name = "retrieve_teaching_knowledge"
    arguments = {"query": query, "topK": topK, "metadataFilter": metadataFilter}
    runtime.stream_writer({"event": "tool_start", "toolName": tool_name, "arguments": arguments})
    started = time.monotonic()
    status = "SUCCEEDED"
    result: Any = None
    try:
        result = await context.knowledge.retrieve(query, topK, metadataFilter)
        context.used_tools.append(tool_name)
        context.rag_hit_count += int(result["hitCount"])
        context.metrics.rag_hits.observe(result["hitCount"])
        return result
    except Exception as exception:
        status = "FAILED"
        result = {"error": str(exception)}
        raise
    finally:
        duration_ms = int((time.monotonic() - started) * 1000)
        context.tool_call_count += 1
        await context.database.record_tool(
            context.request.runId, tool_name, arguments, result, status, duration_ms
        )
        context.metrics.tool_calls.labels(tool=tool_name, status=status).inc()
        runtime.stream_writer(
            {
                "event": "tool_result",
                "toolName": tool_name,
                "status": status,
                "durationMs": duration_ms,
                "result": result,
            }
        )


@tool(
    "get_class_dashboard",
    args_schema=ClassDashboardArguments,
    description="读取当前教师有权查看的班级学情统计、薄弱知识点、趋势和学生预警。",
)
async def get_class_dashboard(
    classId: int, runtime: ToolRuntime[AgentContext], days: int = 30
) -> dict[str, Any]:
    return await _invoke_java(
        runtime.context,
        runtime,
        "get_class_dashboard",
        {"classId": classId, "days": days},
    )


@tool(
    "search_questions",
    args_schema=SearchQuestionsArguments,
    description="从已审核启用且有标准答案的固定题库检索作业候选题。",
)
async def search_questions(
    runtime: ToolRuntime[AgentContext],
    keyword: str | None = None,
    knowledgePointId: int | None = None,
    questionType: str | None = None,
    difficulty: int | None = None,
    size: int = 10,
) -> dict[str, Any]:
    arguments = {
        key: value
        for key, value in {
            "keyword": keyword,
            "knowledgePointId": knowledgePointId,
            "questionType": questionType,
            "difficulty": difficulty,
            "size": size,
        }.items()
        if value is not None
    }
    return await _invoke_java(runtime.context, runtime, "search_questions", arguments)


@tool(
    "create_assignment_draft",
    args_schema=AssignmentDraftArguments,
    description="生成等待教师在界面中明确确认的作业草稿；该工具本身不会发布作业。",
)
async def create_assignment_draft(
    classId: int,
    title: str,
    assignmentType: str,
    startAt: str,
    dueAt: str,
    questions: list[AssignmentQuestion],
    runtime: ToolRuntime[AgentContext],
    description: str | None = None,
    settings: str | None = None,
) -> dict[str, Any]:
    arguments = {
        key: value
        for key, value in {
            "classId": classId,
            "title": title,
            "description": description,
            "assignmentType": assignmentType,
            "startAt": startAt,
            "dueAt": dueAt,
            "settings": settings,
            "questions": [question.model_dump() for question in questions],
        }.items()
        if value is not None
    }
    return await _invoke_java(runtime.context, runtime, "create_assignment_draft", arguments)


TOOL_REGISTRY: dict[str, BaseTool] = {
    tool.name: tool
    for tool in (
        retrieve_teaching_knowledge,
        get_class_dashboard,
        search_questions,
        create_assignment_draft,
    )
}


def tools_for(allowed_tools: list[str], include_local: bool = True) -> list[BaseTool]:
    """Expose only tools authorized and signed by Java, plus the local read-only RAG tool."""

    names = [name for name in allowed_tools if name in TOOL_REGISTRY and name not in LOCAL_TOOLS]
    if include_local:
        names = [*LOCAL_TOOLS, *names]
    return [TOOL_REGISTRY[name] for name in dict.fromkeys(names)]
