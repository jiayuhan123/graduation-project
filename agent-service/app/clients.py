from __future__ import annotations

from typing import Any

import httpx
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from app.config import Settings
from app.models import JavaResult, RunRequest, ToolInvocation


def create_chat_model(settings: Settings) -> ChatOpenAI:
    """Create the OpenAI-compatible model used by LangChain and LangGraph."""

    return ChatOpenAI(
        model=settings.model_name or "model-not-configured",
        api_key=settings.model_api_key or "key-not-configured",
        base_url=settings.model_base_url,
        timeout=settings.model_timeout_seconds,
        max_retries=2,
        temperature=0.2,
        streaming=True,
        stream_usage=False,
        use_responses_api=False,
    )


def create_embeddings(settings: Settings) -> OpenAIEmbeddings:
    """Create embeddings without OpenAI tokenizer assumptions for compatible providers."""

    return OpenAIEmbeddings(
        model=settings.embedding_model or "embedding-not-configured",
        api_key=settings.model_api_key or "key-not-configured",
        base_url=settings.model_base_url,
        dimensions=settings.embedding_dimensions,
        chunk_size=settings.embedding_batch_size,
        request_timeout=settings.model_timeout_seconds,
        max_retries=2,
        tiktoken_enabled=False,
        check_embedding_ctx_length=False,
    )


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
        try:
            body = JavaResult.model_validate(response.json())
        except Exception as exception:
            raise RuntimeError(
                f"Java 工具返回格式不正确：HTTP {response.status_code}"
            ) from exception
        if response.is_error or body.code != 200:
            raise RuntimeError(body.message or f"Java 工具调用失败：HTTP {response.status_code}")
        if not isinstance(body.data, dict) or "data" not in body.data:
            raise RuntimeError("Java 工具返回格式不正确")
        return body.data["data"]

    async def close(self) -> None:
        await self._client.aclose()
