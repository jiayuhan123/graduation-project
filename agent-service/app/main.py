"""FastAPI 应用入口、生命周期装配和 HTTP/SSE 路由。

本模块是服务的 composition root：启动时创建数据库、Redis、模型、LangGraph、RAG 和
TeachingAgent，并放入 `app.state` 供路由复用；关闭时按相反顺序释放资源。具体推理、工具
和持久化逻辑分别留在对应模块，路由层只处理协议、安全入口与错误映射。
"""

from __future__ import annotations

import asyncio
import logging
import secrets
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager, suppress
from typing import Any
from uuid import UUID

from fastapi import FastAPI, Header, HTTPException, Request, Response, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, StreamingResponse
from prometheus_client import CONTENT_TYPE_LATEST
from psycopg.rows import dict_row
from psycopg_pool import AsyncConnectionPool
from redis.asyncio import Redis

from app.agent import AgentFactory, TeachingAgent, sse
from app.clients import JavaToolClient, create_chat_model, create_embeddings
from app.config import Settings
from app.limiter import RunLimiter
from app.memory import AgentMemory
from app.models import (
    FeedbackRequest,
    ForgetMemoryRequest,
    KnowledgeDocumentView,
    KnowledgeUpsertRequest,
    RunRequest,
)
from app.observability import AgentMetrics
from app.rag import KnowledgeService
from app.storage import Database

# 模块被 Uvicorn 导入时读取并校验一次配置；修改环境变量后需要重启进程。
settings = Settings.from_environment()
settings.validate()
metrics = AgentMetrics()
logger = logging.getLogger(__name__)


async def cleanup_loop(database: Database) -> None:
    """每六小时清理一次超过保留期的运行审计。

    长期语义记忆的 TTL 由 LangGraph Store 自己的 sweeper 负责，这个任务只清理项目表。
    """

    while True:
        await asyncio.sleep(6 * 60 * 60)
        await database.cleanup(settings.memory_retention_days, settings.trace_retention_days)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    """在整个 FastAPI 进程生命周期内创建并管理共享依赖。"""

    # 这里只构造客户端对象；真正连接会在后面的 open/ping/setup 中建立。
    database = Database(settings)
    redis = Redis.from_url(
        settings.redis_url,
        decode_responses=True,
        health_check_interval=30,
        socket_connect_timeout=5,
        socket_timeout=5,
    )
    model = create_chat_model(settings)
    embeddings = create_embeddings(settings)
    java_tools = JavaToolClient(settings)
    # LangGraph 官方 PostgreSQL 适配器要求 psycopg pool；领域审计则使用 Database 的 asyncpg pool。
    graph_pool = AsyncConnectionPool(
        settings.psycopg_database_url,
        min_size=settings.database_pool_min_size,
        max_size=settings.database_pool_max_size,
        kwargs={
            "autocommit": True,
            "prepare_threshold": 0,
            "row_factory": dict_row,
        },
        open=False,
        name="agent-langgraph",
    )
    cleanup_task: asyncio.Task[None] | None = None
    knowledge: KnowledgeService | None = None
    graph_store: Any | None = None
    try:
        # 项目迁移必须先完成，之后 LangGraph/PGVector 才能在稳定 schema 上初始化自己的表。
        await database.open()
        await redis.ping()
        await graph_pool.open()
        await graph_pool.wait()
        from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
        from langgraph.store.postgres.aio import AsyncPostgresStore

        # Checkpointer 保存对话线程的图状态；Store 保存带向量索引和 TTL 的长期语义记忆。
        checkpointer = AsyncPostgresSaver(graph_pool)
        graph_store = AsyncPostgresStore(
            graph_pool,
            index={
                "dims": settings.embedding_dimensions,
                "embed": embeddings,
                "fields": ["content"],
                "distance_type": "cosine",
            },
            ttl={
                "refresh_on_read": True,
                "omit_expired": True,
                "default_ttl": float(settings.memory_retention_days * 24 * 60),
                "sweep_interval_minutes": 60,
            },
        )
        # setup() 幂等创建/升级框架表；不能用项目 SQL 猜测并手工维护这些内部表。
        await checkpointer.setup()
        await graph_store.setup()
        await graph_store.start_ttl_sweeper()
        # RAG 服务会确保 chunk 向量表和 HNSW 索引存在，并编译检索子图。
        knowledge = await KnowledgeService.create(settings, database, embeddings, model)
        # ai_knowledge_chunk 由上一步动态创建；此时统一写入六张 AI 表的表/字段注释。
        await database.apply_schema_comments()
        memory = AgentMemory(settings, graph_store, checkpointer)
        factory = AgentFactory(settings, model, checkpointer, graph_store)
        # app.state 保存应用级单例，所有请求共享连接池、模型适配器和已编译 Agent 图。
        app.state.database = database
        app.state.redis = redis
        app.state.memory = memory
        app.state.knowledge = knowledge
        app.state.limiter = RunLimiter(
            redis, settings.max_requests_per_minute, settings.max_concurrent_runs
        )
        app.state.agent = TeachingAgent(
            settings,
            factory,
            java_tools,
            memory,
            knowledge,
            database,
            metrics,
        )
        cleanup_task = asyncio.create_task(cleanup_loop(database), name="agent-data-cleanup")
        # yield 之前是 startup；运行到这里 Uvicorn 才开始正常接收请求。
        yield
    finally:
        # shutdown 必须容忍部分初始化失败，所以可选资源都先判断再关闭。
        if cleanup_task:
            cleanup_task.cancel()
            with suppress(asyncio.CancelledError):
                await cleanup_task
        if graph_store is not None:
            await graph_store.stop_ttl_sweeper(timeout=5)
        if knowledge is not None:
            await knowledge.close()
        await java_tools.close()
        await graph_pool.close()
        await redis.aclose()
        await database.close()


app = FastAPI(
    title="Primary Math Teaching Agent",
    version="0.2.0",
    docs_url="/docs" if settings.docs_enabled else None,
    openapi_url="/openapi.json" if settings.docs_enabled else None,
    redoc_url=None,
    lifespan=lifespan,
)


@app.exception_handler(RequestValidationError)
async def request_validation_handler(
    request: Request, exception: RequestValidationError
) -> JSONResponse:
    """记录可诊断的字段错误，但不把请求正文或内部令牌写入日志。"""
    safe_errors = [
        {
            "type": error.get("type", "validation_error"),
            "loc": [str(item) for item in error.get("loc", ())],
            "msg": error.get("msg", "Invalid request"),
        }
        for error in exception.errors()
    ]
    logger.warning(
        "request validation failed path=%s errors=%s",
        request.url.path,
        safe_errors,
    )
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
        content={"detail": safe_errors},
    )


@app.get("/health")
async def health() -> dict[str, str]:
    """Liveness：只证明 FastAPI 进程和事件循环仍能响应。"""

    return {"status": "UP"}


@app.get("/ready")
async def ready(request: Request) -> dict[str, Any]:
    """Readiness：检查数据库、Redis 和关键配置是否具备接收请求的条件。"""

    database_ok = await request.app.state.database.ping()
    try:
        redis_ok = bool(await request.app.state.redis.ping())
    except Exception as exception:  # noqa: BLE001 - readiness 汇总所有依赖故障。
        logger.warning("redis readiness check failed error_type=%s", type(exception).__name__)
        redis_ok = False
    # 模型、Embedding 和凭证这里只检查“已配置”，不会产生有成本的真实外部探测。
    ready_now = (
        database_ok
        and redis_ok
        and settings.model_configured
        and settings.embedding_configured
        and settings.credentials_configured
    )
    if not ready_now:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "database": database_ok,
                "redis": redis_ok,
                "modelConfigured": settings.model_configured,
                "embeddingConfigured": settings.embedding_configured,
                "credentialsConfigured": settings.credentials_configured,
            },
        )
    return {"status": "READY", "database": True, "redis": True}


@app.get("/metrics", include_in_schema=False)
async def prometheus_metrics() -> Response:
    """返回 Prometheus 文本指标；不放入对外 OpenAPI 文档。"""

    return Response(content=metrics.render(), media_type=CONTENT_TYPE_LATEST)


def verify_bridge_token(supplied: str | None) -> None:
    """验证 Java 调用 Python 使用的固定桥接令牌。"""

    if not settings.bridge_token:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI_AGENT_BRIDGE_TOKEN is not configured",
        )
    # compare_digest 使用常量时间比较，避免普通字符串比较暴露前缀匹配时序。
    if supplied is None or not secrets.compare_digest(settings.bridge_token, supplied):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")


@app.post("/v1/agent/runs/stream")
async def stream_run(
    request_body: RunRequest,
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> StreamingResponse:
    """校验并启动一次 Agent，以 SSE 持续返回元数据、工具和回答事件。"""

    verify_bridge_token(bridge_token)
    try:
        await request.app.state.limiter.check_rate(request_body.actor.userId)
    except RuntimeError as exception:
        raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS, detail=str(exception))

    async def events() -> AsyncIterator[str]:
        """在并发槽位内消费 Agent 异步生成器，并兜底流建立后的异常。"""

        try:
            async with request.app.state.limiter:
                async for event in request.app.state.agent.run(request_body):
                    yield event
        except Exception as exception:  # noqa: BLE001 - SSE 建立后必须转成事件。
            # 响应头已经发出，无法再改成 HTTP 500，只能发送协议内 error 事件。
            logger.warning(
                "agent stream failed run_id=%s error_type=%s",
                request_body.runId,
                type(exception).__name__,
            )
            yield sse("error", {"message": "Agent 运行失败，请稍后重试"})

    return StreamingResponse(
        events(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@app.post("/v1/knowledge/documents", response_model=KnowledgeDocumentView)
async def upsert_knowledge(
    body: KnowledgeUpsertRequest,
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> KnowledgeDocumentView:
    """新增知识或按 sourceUri 替换内容，并等待向量索引完成后返回。"""

    verify_bridge_token(bridge_token)
    return await request.app.state.knowledge.ingest(body)


@app.get("/v1/knowledge/documents", response_model=list[KnowledgeDocumentView])
async def list_knowledge(
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> list[KnowledgeDocumentView]:
    """列出全部未软删除的知识文档摘要。"""

    verify_bridge_token(bridge_token)
    return await request.app.state.database.list_knowledge()


@app.delete("/v1/knowledge/documents/{document_id}")
async def delete_knowledge(
    document_id: UUID,
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> dict[str, bool]:
    """软删除知识主记录并删除所属向量 chunk。"""

    verify_bridge_token(bridge_token)
    deleted = await request.app.state.knowledge.delete(document_id)
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Document not found")
    return {"deleted": True}


@app.post("/v1/agent/feedback")
async def save_feedback(
    body: FeedbackRequest,
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> dict[str, bool]:
    """保存运行所有者的正负反馈，并更新实时指标。"""

    verify_bridge_token(bridge_token)
    saved = await request.app.state.database.save_feedback(body)
    if not saved:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Agent run not found")
    metrics.feedback.labels(rating="positive" if body.rating == 1 else "negative").inc()
    return {"saved": True}


@app.post("/v1/agent/memory/forget")
async def forget_memory(
    body: ForgetMemoryRequest,
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> dict[str, int | bool]:
    """删除指定用户会话的长期记忆和所有角色 checkpoint。"""

    verify_bridge_token(bridge_token)
    deleted = await request.app.state.memory.forget(body.actorId, body.conversationId)
    return {"forgotten": True, "deletedMemories": deleted}


@app.get("/v1/agent/quality")
async def quality_summary(
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> dict[str, Any]:
    """返回最近 30 天 Agent 运行和反馈质量统计。"""

    verify_bridge_token(bridge_token)
    return await request.app.state.database.quality_summary()
