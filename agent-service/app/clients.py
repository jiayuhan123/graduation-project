from __future__ import annotations

import asyncio
import json
from collections.abc import AsyncIterator
from typing import Any

import httpx

from app.config import Settings
from app.models import JavaResult, RunRequest, ToolInvocation


class ModelClient:
    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self._settings = settings
        self._client = client or httpx.AsyncClient(timeout=settings.model_timeout_seconds)

    async def plan(
        self, messages: list[dict[str, Any]], tools: list[dict[str, Any]]
    ) -> dict[str, Any]:
        self._ensure_configured()
        response = await self._post_with_retry(
            f"{self._settings.model_base_url}/chat/completions",
            {
                "model": self._settings.model_name,
                "messages": messages,
                "tools": tools,
                "tool_choice": "auto",
                "temperature": 0.2,
                "stream": False,
            },
        )
        response.raise_for_status()
        body = response.json()
        return body["choices"][0]["message"]

    async def stream_answer(self, messages: list[dict[str, Any]]) -> AsyncIterator[str]:
        self._ensure_configured()
        async with self._client.stream(
            "POST",
            f"{self._settings.model_base_url}/chat/completions",
            headers=self._headers(),
            json={
                "model": self._settings.model_name,
                "messages": messages,
                "temperature": 0.2,
                "stream": True,
            },
        ) as response:
            response.raise_for_status()
            async for line in response.aiter_lines():
                if not line.startswith("data:"):
                    continue
                data = line.removeprefix("data:").strip()
                if not data or data == "[DONE]":
                    continue
                payload = json.loads(data)
                delta = payload.get("choices", [{}])[0].get("delta", {}).get("content")
                if delta:
                    yield delta

    def _headers(self) -> dict[str, str]:
        return {
            "Authorization": f"Bearer {self._settings.model_api_key}",
            "Content-Type": "application/json",
        }

    def _ensure_configured(self) -> None:
        if not self._settings.model_configured:
            raise RuntimeError("AI_MODEL_API_KEY 或 AI_MODEL_NAME 尚未配置")

    async def close(self) -> None:
        await self._client.aclose()

    async def _post_with_retry(self, url: str, payload: dict[str, Any]) -> httpx.Response:
        last_error: Exception | None = None
        for attempt in range(3):
            try:
                response = await self._client.post(url, headers=self._headers(), json=payload)
                if response.status_code not in {429, 500, 502, 503, 504}:
                    return response
                last_error = RuntimeError(f"model transient status {response.status_code}")
            except (httpx.ConnectError, httpx.ReadTimeout) as exception:
                last_error = exception
            if attempt < 2:
                await asyncio.sleep(0.25 * (2**attempt))
        raise RuntimeError("模型服务连续调用失败") from last_error


class EmbeddingClient:
    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self._settings = settings
        self._client = client or httpx.AsyncClient(timeout=settings.model_timeout_seconds)

    async def embed(self, texts: list[str]) -> list[list[float]]:
        if not self._settings.embedding_configured:
            raise RuntimeError("AI_EMBEDDING_MODEL 尚未配置")
        if not texts:
            return []
        vectors: list[list[float]] = []
        for index in range(0, len(texts), self._settings.embedding_batch_size):
            vectors.extend(
                await self._embed_batch(texts[index : index + self._settings.embedding_batch_size])
            )
        if len(vectors) != len(texts):
            raise RuntimeError("Embedding 服务返回数量不匹配")
        if any(len(vector) != self._settings.embedding_dimensions for vector in vectors):
            raise RuntimeError("Embedding 维度与 AI_EMBEDDING_DIMENSIONS 不匹配")
        return vectors

    async def _embed_batch(self, texts: list[str]) -> list[list[float]]:
        last_error: Exception | None = None
        for attempt in range(3):
            try:
                response = await self._client.post(
                    f"{self._settings.model_base_url}/embeddings",
                    headers={
                        "Authorization": f"Bearer {self._settings.model_api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": self._settings.embedding_model,
                        "input": texts,
                        "dimensions": self._settings.embedding_dimensions,
                    },
                )
                if response.status_code not in {429, 500, 502, 503, 504}:
                    response.raise_for_status()
                    items = sorted(response.json().get("data", []), key=lambda item: item["index"])
                    return [item["embedding"] for item in items]
                last_error = RuntimeError(f"embedding transient status {response.status_code}")
            except (httpx.ConnectError, httpx.ReadTimeout) as exception:
                last_error = exception
            if attempt < 2:
                await asyncio.sleep(0.25 * (2**attempt))
        raise RuntimeError("Embedding 服务连续调用失败") from last_error

    async def close(self) -> None:
        await self._client.aclose()


class JavaToolClient:
    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self._settings = settings
        self._client = client or httpx.AsyncClient(timeout=settings.model_timeout_seconds)

    async def invoke(
        self,
        run: RunRequest,
        tool_name: str,
        arguments: dict[str, Any],
    ) -> Any:
        response = await self._client.post(
            f"{self._settings.java_tool_base_url}/internal/ai/tools/invoke",
            headers={
                "X-Internal-Token": self._settings.java_internal_api_token,
                "X-Agent-Tool-Token": run.toolToken,
            },
            json=ToolInvocation(
                runId=run.runId, toolName=tool_name, arguments=arguments
            ).model_dump(mode="json"),
        )
        body = JavaResult.model_validate(response.json())
        if response.is_error or body.code != 200:
            raise RuntimeError(body.message or f"Java 工具调用失败：HTTP {response.status_code}")
        if not isinstance(body.data, dict) or "data" not in body.data:
            raise RuntimeError("Java 工具返回格式不正确")
        return body.data["data"]

    async def close(self) -> None:
        await self._client.aclose()
