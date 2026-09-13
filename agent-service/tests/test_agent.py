from __future__ import annotations

import json
from collections.abc import AsyncIterator
from uuid import uuid4

import pytest

from app.agent import TeachingAgent
from app.config import Settings
from app.memory import ConversationMemory
from app.models import Actor, RunRequest


class FakeModelClient:
    def __init__(self) -> None:
        self.plans = 0

    async def plan(self, messages: list[dict], tools: list[dict]) -> dict:
        self.plans += 1
        if self.plans == 1:
            return {
                "content": None,
                "tool_calls": [
                    {
                        "id": "call-1",
                        "type": "function",
                        "function": {
                            "name": "get_class_dashboard",
                            "arguments": '{"classId":12,"days":7}',
                        },
                    }
                ],
            }
        return {"content": "ready"}

    async def stream_answer(self, messages: list[dict]) -> AsyncIterator[str]:
        yield "班级正确率"
        yield "为 80%。"


class FakeToolClient:
    async def invoke(self, run: RunRequest, tool_name: str, arguments: dict) -> dict:
        assert tool_name in run.actor.allowedTools
        return {"classId": 12, "metrics": {"accuracyRate": 0.8}}


@pytest.mark.asyncio
async def test_agent_executes_authorized_tool_and_streams_answer() -> None:
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
    agent = TeachingAgent(
        settings,
        FakeModelClient(),  # type: ignore[arg-type]
        FakeToolClient(),  # type: ignore[arg-type]
        ConversationMemory(),
    )

    events = [event async for event in agent.run(request)]

    names = [event.splitlines()[0] for event in events]
    assert "event: tool_start" in names
    assert "event: tool_result" in names
    assert names.count("event: delta") == 2
    done = json.loads(events[-1].split("data: ", 1)[1])
    assert done["toolCalls"] == ["get_class_dashboard"]
