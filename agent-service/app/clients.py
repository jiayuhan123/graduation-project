"""封装 Agent 服务访问的三个外部客户端。

对话模型和 Embedding 使用 LangChain 的 OpenAI-compatible 适配器；业务工具使用 httpx
回调 Java。把创建逻辑集中在这里，可以让上层 Agent 只依赖抽象能力而不关心 HTTP 细节。
"""

from __future__ import annotations

from typing import Any

import httpx
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from app.config import Settings
from app.models import JavaResult, RunRequest, ToolInvocation


def create_chat_model(settings: Settings) -> ChatOpenAI:
    """创建供 LangChain/LangGraph 使用的 OpenAI-compatible 流式对话模型。"""

    # 未配置时仍构造占位客户端，使应用能启动并通过 /health；/ready 会报告配置不完整。
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
    """创建 Embedding 客户端，并避免对兼容厂商套用 OpenAI tokenizer 假设。"""

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
    """调用 Java 可信业务边界的异步 HTTP 客户端。

    Python 只负责决定何时调用工具；身份、动态授权和最终业务执行由 Java 负责。客户端同时
    发送固定内部令牌和本次运行的短期工具令牌，Java 会再次验证两者。
    """

    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None) -> None:
        self._settings = settings
        self._client = client or httpx.AsyncClient(timeout=settings.model_timeout_seconds)

    async def invoke(
        self,
        run: RunRequest,
        tool_name: str,
        arguments: dict[str, Any],
    ) -> Any:
        """调用一个 Java 白名单工具，校验统一响应后返回真正的工具数据。"""

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
        # 即使 HTTP 为 200，也必须校验 Java 的统一业务响应结构和业务 code。
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
        """应用关闭时释放 httpx 连接池。"""

        await self._client.aclose()
