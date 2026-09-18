from __future__ import annotations

import argparse
import asyncio
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from uuid import uuid4

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.store.memory import InMemoryStore

from app.agent import AgentFactory, TeachingAgent
from app.clients import create_chat_model
from app.config import Settings
from app.models import Actor, RunRequest
from app.observability import AgentMetrics


@dataclass(frozen=True, slots=True)
class EvaluationCase:
    case_id: str
    message: str
    allowed_tools: list[str]
    required_tools: set[str]
    forbidden_tools: set[str]
    required_phrases: list[str]
    forbidden_phrases: list[str]
    tool_fixtures: dict[str, dict[str, Any]]
    rag_fixture: dict[str, Any] | None


class FixtureToolClient:
    def __init__(self, fixtures: dict[str, dict[str, Any]]) -> None:
        self._fixtures = fixtures

    async def invoke(
        self, run: RunRequest, tool_name: str, _arguments: dict[str, Any]
    ) -> dict[str, Any]:
        if tool_name not in run.actor.allowedTools:
            raise RuntimeError(f"unauthorized tool in evaluation: {tool_name}")
        return self._fixtures.get(tool_name, {"evaluationFixture": True})


class FixtureKnowledgeService:
    def __init__(self, fixture: dict[str, Any] | None) -> None:
        self._fixture = fixture or {"hitCount": 0, "sources": []}

    async def retrieve(self, query: str, *_args: Any) -> dict[str, Any]:
        return {"query": query, **self._fixture}


class EvaluationMemory:
    async def recall(self, *_args: Any) -> list[dict[str, Any]]:
        return []

    async def remember(self, *_args: Any) -> None:
        return None

    @staticmethod
    def thread_id(actor_id: int, conversation_id: Any, active_role: str) -> str:
        return f"eval:{actor_id}:{conversation_id}:{active_role}"


class EvaluationDatabase:
    async def start_run(self, *_args: Any) -> None:
        return None

    async def record_tool(self, *_args: Any) -> None:
        return None

    async def finish_run(self, *_args: Any) -> None:
        return None


def load_cases(path: Path) -> list[EvaluationCase]:
    cases: list[EvaluationCase] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        item = json.loads(line)
        cases.append(
            EvaluationCase(
                case_id=item["id"],
                message=item["message"],
                allowed_tools=item["allowedTools"],
                required_tools=set(item.get("requiredTools", [])),
                forbidden_tools=set(item.get("forbiddenTools", [])),
                required_phrases=item.get("requiredPhrases", []),
                forbidden_phrases=item.get("forbiddenPhrases", []),
                tool_fixtures=item.get("toolFixtures", {}),
                rag_fixture=item.get("ragFixture"),
            )
        )
    return cases


def score_case(case: EvaluationCase, tool_calls: set[str], answer: str) -> tuple[bool, list[str]]:
    failures: list[str] = []
    missing = case.required_tools.difference(tool_calls)
    forbidden = case.forbidden_tools.intersection(tool_calls)
    if missing:
        failures.append(f"missing tools: {','.join(sorted(missing))}")
    if forbidden:
        failures.append(f"forbidden tools: {','.join(sorted(forbidden))}")
    for phrase in case.required_phrases:
        if phrase not in answer:
            failures.append(f"missing phrase: {phrase}")
    for phrase in case.forbidden_phrases:
        if phrase in answer:
            failures.append(f"forbidden phrase: {phrase}")
    return not failures, failures


async def evaluate_case(settings: Settings, case: EvaluationCase) -> dict[str, Any]:
    request = RunRequest(
        runId=uuid4(),
        conversationId=uuid4(),
        message=case.message,
        actor=Actor(
            userId=1,
            displayName="评测教师",
            activeRole="teacher",
            roles=["teacher"],
            allowedTools=case.allowed_tools,
        ),
        toolToken="evaluation-token",
    )
    model = create_chat_model(settings)
    factory = AgentFactory(settings, model, InMemorySaver(), InMemoryStore())  # type: ignore[arg-type]
    agent = TeachingAgent(
        settings,
        factory,
        FixtureToolClient(case.tool_fixtures),  # type: ignore[arg-type]
        EvaluationMemory(),  # type: ignore[arg-type]
        FixtureKnowledgeService(case.rag_fixture),  # type: ignore[arg-type]
        EvaluationDatabase(),  # type: ignore[arg-type]
        AgentMetrics(),
    )
    calls: set[str] = set()
    answer_parts: list[str] = []
    try:
        async for event in agent.run(request):
            lines = event.splitlines()
            event_name = lines[0].removeprefix("event: ")
            data = json.loads(lines[1].removeprefix("data: "))
            if event_name == "tool_start":
                calls.add(data["toolName"])
            elif event_name == "delta":
                answer_parts.append(data["content"])
    finally:
        await model.root_async_client.close()
    answer = "".join(answer_parts)
    passed, failures = score_case(case, calls, answer)
    return {
        "id": case.case_id,
        "passed": passed,
        "toolCalls": sorted(calls),
        "failures": failures,
        "answer": answer,
    }


async def run(path: Path, threshold: float) -> int:
    settings = Settings.from_environment()
    if not settings.model_configured:
        raise RuntimeError("运行评测前必须配置 AI_MODEL_API_KEY 和 AI_MODEL_NAME")
    cases = load_cases(path)
    results = [await evaluate_case(settings, case) for case in cases]
    passed = sum(1 for item in results if item["passed"])
    pass_rate = passed / len(results) if results else 0.0
    print(json.dumps({"passRate": pass_rate, "results": results}, ensure_ascii=False, indent=2))
    return 0 if pass_rate >= threshold else 1


def main() -> None:
    parser = argparse.ArgumentParser(description="Run deterministic teaching Agent evaluations")
    parser.add_argument("dataset", type=Path)
    parser.add_argument("--threshold", type=float, default=0.8)
    args = parser.parse_args()
    raise SystemExit(asyncio.run(run(args.dataset, args.threshold)))


if __name__ == "__main__":
    main()
