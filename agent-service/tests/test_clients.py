from __future__ import annotations

import json

import httpx
import pytest

from app.clients import EmbeddingClient, ModelClient
from app.config import Settings


@pytest.mark.asyncio
async def test_model_stream_ignores_usage_chunk_with_empty_choices() -> None:
    async def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            headers={"Content-Type": "text/event-stream"},
            text=(
                'data: {"choices":[{"delta":{"content":"分析完成"}}]}\n\n'
                'data: {"choices":[],"usage":{"total_tokens":42}}\n\n'
                "data: [DONE]\n\n"
            ),
        )

    settings = Settings(
        bridge_token="bridge",
        java_tool_base_url="http://java",
        java_internal_api_token="internal",
        model_base_url="http://model/v1",
        model_api_key="key",
        model_name="chat-model",
        model_timeout_seconds=5,
        max_tool_rounds=4,
    )
    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    client = ModelClient(settings, http)

    chunks = [chunk async for chunk in client.stream_answer([{"role": "user", "content": "x"}])]

    assert chunks == ["分析完成"]
    await client.close()


@pytest.mark.asyncio
async def test_embedding_client_batches_and_preserves_order() -> None:
    batches: list[list[str]] = []

    async def handler(request: httpx.Request) -> httpx.Response:
        body = json.loads(request.content)
        batches.append(body["input"])
        return httpx.Response(
            200,
            json={
                "data": [
                    {"index": index, "embedding": [float(index), 0.0, 1.0]}
                    for index, _ in enumerate(body["input"])
                ]
            },
        )

    settings = Settings(
        bridge_token="bridge",
        java_tool_base_url="http://java",
        java_internal_api_token="internal",
        model_base_url="http://model/v1",
        model_api_key="key",
        model_name="chat-model",
        model_timeout_seconds=5,
        max_tool_rounds=4,
        embedding_model="embedding-model",
        embedding_dimensions=3,
        embedding_batch_size=2,
    )
    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    client = EmbeddingClient(settings, http)

    vectors = await client.embed(["a", "b", "c"])

    assert batches == [["a", "b"], ["c"]]
    assert len(vectors) == 3
    await client.close()
