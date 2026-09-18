from __future__ import annotations

import json
from typing import Any
from uuid import uuid4

import pytest
from langchain_core.messages import AIMessage, AIMessageChunk

from app.agent import TeachingAgent
from app.config import Settings
from app.models import Actor, RunRequest
from app.observability import AgentMetrics


class FakeGraph:
    async def astream(self, _input: Any, **kwargs: Any) -> Any:
        context = kwargs["context"]
        context.used_tools.append("get_class_dashboard")
        yield {
            "type": "custom",
            "data": {"event": "tool_start", "toolName": "get_class_dashboard"},
        }
        yield {
            "type": "custom",
            "data": {
                "event": "tool_result",
                "toolName": "get_class_dashboard",
                "status": "SUCCEEDED",
                "result": {"accuracyRate": 0.8},
            },
        }
        yield {"type": "messages", "data": (AIMessageChunk(content="班级正确率"), {})}
        yield {"type": "messages", "data": (AIMessageChunk(content="为 80%。"), {})}
        yield {
            "type": "values",
            "data": {"messages": [AIMessage(content="班级正确率为 80%。")]},
        }


class FakeFactory:
    def for_tools(self, allowed_tools: list[str]) -> FakeGraph:
        assert allowed_tools == ["get_class_dashboard"]
        return FakeGraph()


class FakeMemory:
    remembered = False

    async def recall(self, *_args: Any) -> list[dict[str, Any]]:
        return []

    async def remember(self, *_args: Any) -> None:
        self.remembered = True

    @staticmethod
    def thread_id(actor_id: int, conversation_id: Any, active_role: str) -> str:
        return f"{actor_id}:{conversation_id}:{active_role}"


class FakeDatabase:
    finished: tuple[Any, ...] | None = None

    async def start_run(self, *_args: Any) -> None:
        return None

    async def finish_run(self, *args: Any) -> None:
        self.finished = args


@pytest.mark.asyncio
async def test_agent_adapts_langgraph_stream_to_existing_sse_contract() -> None:
    settings = Settings(
        bridge_token="bridge",
        java_tool_base_url="http://java",
        java_internal_api_token="internal",
        model_base_url="http://model/v1",
        model_api_key="key",
        model_name="test-model",
        model_timeout_seconds=5,
        max_tool_rounds=4,
    )
    request = RunRequest(
        runId=uuid4(),
        conversationId=uuid4(),
        message="分析 12 班",
        actor=Actor(
            userId=7,
            displayName="王老师",
            activeRole="teacher",
            roles=["teacher"],
            allowedTools=["get_class_dashboard"],
        ),
        toolToken="signed-token",
    )
    memory = FakeMemory()
    database = FakeDatabase()
    agent = TeachingAgent(
        settings,
        FakeFactory(),  # type: ignore[arg-type]
        object(),  # type: ignore[arg-type]
        memory,  # type: ignore[arg-type]
        object(),  # type: ignore[arg-type]
        database,  # type: ignore[arg-type]
        AgentMetrics(),
    )

    events = [event async for event in agent.run(request)]

    names = [event.splitlines()[0] for event in events]
    assert names[0] == "event: metadata"
    assert "event: tool_start" in names
    assert "event: tool_result" in names
    assert names.count("event: delta") == 2
    done = json.loads(events[-1].split("data: ", 1)[1])
    assert done["toolCalls"] == ["get_class_dashboard"]
    assert memory.remembered
    assert database.finished is not None
