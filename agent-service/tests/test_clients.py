from __future__ import annotations

import httpx
import pytest
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from app.clients import JavaToolClient, create_chat_model, create_embeddings
from app.config import Settings
from app.models import Actor, RunRequest


def settings() -> Settings:
    return Settings(
        bridge_token="bridge",
        java_tool_base_url="http://java",
        java_internal_api_token="internal",
        model_base_url="http://model/v1",
        model_api_key="key",
        model_name="chat-model",
        model_timeout_seconds=5,
        max_tool_rounds=4,
        embedding_model="embedding-model",
        embedding_dimensions=1024,
        embedding_batch_size=2,
    )


def test_langchain_clients_use_openai_compatible_configuration() -> None:
    model = create_chat_model(settings())
    embeddings = create_embeddings(settings())

    assert isinstance(model, ChatOpenAI)
    assert model.model_name == "chat-model"
    assert isinstance(embeddings, OpenAIEmbeddings)
    assert embeddings.model == "embedding-model"
    assert embeddings.dimensions == 1024
    assert embeddings.tiktoken_enabled is False


@pytest.mark.asyncio
async def test_java_tool_client_preserves_signed_bridge_contract() -> None:
    captured: httpx.Request | None = None

    async def handler(request: httpx.Request) -> httpx.Response:
        nonlocal captured
        captured = request
        return httpx.Response(200, json={"code": 200, "message": "ok", "data": {"data": {"x": 1}}})

    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    client = JavaToolClient(settings(), http)
    request = RunRequest(
        runId="77777777-7777-7777-7777-777777777777",
        conversationId="88888888-8888-8888-8888-888888888888",
        message="test",
        actor=Actor(
            userId=7,
            activeRole="teacher",
            roles=["teacher"],
            allowedTools=["get_class_dashboard"],
        ),
        toolToken="signed-token",
    )

    result = await client.invoke(request, "get_class_dashboard", {"classId": 12})

    assert result == {"x": 1}
    assert captured is not None
    assert captured.headers["X-Agent-Tool-Token"] == "signed-token"
    assert captured.headers["X-Internal-Token"] == "internal"
    await client.close()
