from __future__ import annotations

import asyncio
import logging
import secrets
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager, suppress
from typing import Any
from uuid import UUID

from fastapi import FastAPI, Header, HTTPException, Request, Response, status
from fastapi.responses import StreamingResponse
from prometheus_client import CONTENT_TYPE_LATEST
from redis.asyncio import Redis

from app.agent import TeachingAgent, sse
from app.clients import EmbeddingClient, JavaToolClient, ModelClient
from app.config import Settings
from app.limiter import RunLimiter
from app.memory import RedisConversationMemory
from app.models import (
    FeedbackRequest,
    ForgetMemoryRequest,
    KnowledgeDocumentView,
    KnowledgeUpsertRequest,
    RunRequest,
)
from app.observability import AgentMetrics
from app.rag import KnowledgeService, SemanticMemory
from app.storage import Database

settings = Settings.from_environment()
settings.validate()
metrics = AgentMetrics()
logger = logging.getLogger(__name__)


async def cleanup_loop(database: Database) -> None:
    while True:
        await asyncio.sleep(6 * 60 * 60)
        await database.cleanup(settings.memory_retention_days, settings.trace_retention_days)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    database = Database(settings)
    redis = Redis.from_url(
        settings.redis_url,
        decode_responses=True,
        health_check_interval=30,
        socket_connect_timeout=5,
        socket_timeout=5,
    )
    model = ModelClient(settings)
    embeddings = EmbeddingClient(settings)
    java_tools = JavaToolClient(settings)
    cleanup_task: asyncio.Task[None] | None = None
    try:
        await database.open()
        await redis.ping()
        memory = RedisConversationMemory(
            redis,
            settings.conversation_max_messages,
            settings.conversation_memory_ttl_seconds,
        )
        knowledge = KnowledgeService(settings, database, embeddings)
        semantic_memory = SemanticMemory(settings, database, embeddings)
        app.state.database = database
        app.state.redis = redis
        app.state.memory = memory
        app.state.knowledge = knowledge
        app.state.limiter = RunLimiter(
            redis, settings.max_requests_per_minute, settings.max_concurrent_runs
        )
        app.state.agent = TeachingAgent(
            settings,
            model,
            java_tools,
            memory,
            knowledge,
            semantic_memory,
            database,
            metrics,
        )
        cleanup_task = asyncio.create_task(cleanup_loop(database), name="agent-data-cleanup")
        yield
    finally:
        if cleanup_task:
            cleanup_task.cancel()
            with suppress(asyncio.CancelledError):
                await cleanup_task
        await java_tools.close()
        await embeddings.close()
        await model.close()
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


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "UP"}


@app.get("/ready")
async def ready(request: Request) -> dict[str, Any]:
    database_ok = await request.app.state.database.ping()
    try:
        redis_ok = bool(await request.app.state.redis.ping())
    except Exception as exception:  # noqa: BLE001 - readiness 汇总所有依赖故障。
        logger.warning("redis readiness check failed error_type=%s", type(exception).__name__)
        redis_ok = False
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
    return Response(content=metrics.render(), media_type=CONTENT_TYPE_LATEST)


def verify_bridge_token(supplied: str | None) -> None:
    if not settings.bridge_token:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI_AGENT_BRIDGE_TOKEN is not configured",
        )
    if supplied is None or not secrets.compare_digest(settings.bridge_token, supplied):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")


@app.post("/v1/agent/runs/stream")
async def stream_run(
    request_body: RunRequest,
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> StreamingResponse:
    verify_bridge_token(bridge_token)
    try:
        await request.app.state.limiter.check_rate(request_body.actor.userId)
    except RuntimeError as exception:
        raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS, detail=str(exception))

    async def events() -> AsyncIterator[str]:
        try:
            async with request.app.state.limiter:
                async for event in request.app.state.agent.run(request_body):
                    yield event
        except Exception as exception:  # noqa: BLE001 - SSE 建立后必须转成事件。
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
    verify_bridge_token(bridge_token)
    return await request.app.state.knowledge.ingest(body)


@app.get("/v1/knowledge/documents", response_model=list[KnowledgeDocumentView])
async def list_knowledge(
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> list[KnowledgeDocumentView]:
    verify_bridge_token(bridge_token)
    return await request.app.state.database.list_knowledge()


@app.delete("/v1/knowledge/documents/{document_id}")
async def delete_knowledge(
    document_id: UUID,
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> dict[str, bool]:
    verify_bridge_token(bridge_token)
    deleted = await request.app.state.database.delete_knowledge(document_id)
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Document not found")
    return {"deleted": True}


@app.post("/v1/agent/feedback")
async def save_feedback(
    body: FeedbackRequest,
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> dict[str, bool]:
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
    verify_bridge_token(bridge_token)
    await request.app.state.memory.forget(body.actorId, body.conversationId)
    deleted = await request.app.state.database.forget(body.actorId, body.conversationId)
    return {"forgotten": True, "deletedMemories": deleted}


@app.get("/v1/agent/quality")
async def quality_summary(
    request: Request,
    bridge_token: str | None = Header(default=None, alias="X-AI-Bridge-Token"),
) -> dict[str, Any]:
    verify_bridge_token(bridge_token)
    return await request.app.state.database.quality_summary()
