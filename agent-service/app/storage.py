from __future__ import annotations

import hashlib
import json
import re
from importlib import resources
from typing import Any
from uuid import UUID, uuid4

import asyncpg

from app.config import Settings
from app.models import FeedbackRequest, KnowledgeDocumentView, KnowledgeUpsertRequest

_SECRET_ASSIGNMENT = re.compile(
    r"(?i)\b(api[_\s-]?key|access[_\s-]?token|refresh[_\s-]?token|password|secret)"
    r"(\s*[:=]\s*)([^\s,;]+)"
)
_CHINESE_SECRET_ASSIGNMENT = re.compile(r"(密码|密钥|令牌)(\s*[:：=]\s*)([^\s,;，；]+)")
_BEARER_TOKEN = re.compile(r"(?i)\bBearer\s+[A-Za-z0-9._~+/=-]{12,}")
_OPENAI_STYLE_KEY = re.compile(r"\bsk-[A-Za-z0-9_-]{12,}\b")


def _json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), default=str)


def _bounded_value(value: Any, limit: int) -> Any:
    encoded = _json(value)
    if len(encoded) <= limit:
        return value
    return {"truncated": True, "preview": encoded[: limit - 50]}


def redact_sensitive_text(value: str) -> str:
    """Best-effort removal of credentials accidentally pasted into traces or memory."""

    redacted = _SECRET_ASSIGNMENT.sub(r"\1\2[REDACTED]", value)
    redacted = _CHINESE_SECRET_ASSIGNMENT.sub(r"\1\2[REDACTED]", redacted)
    redacted = _BEARER_TOKEN.sub("Bearer [REDACTED]", redacted)
    return _OPENAI_STYLE_KEY.sub("[REDACTED]", redacted)


def redact_trace_data(value: Any) -> Any:
    if isinstance(value, dict):
        redacted: dict[str, Any] = {}
        for key, item in value.items():
            normalized = key.lower()
            if any(secret in normalized for secret in ("token", "password", "secret")):
                redacted[key] = "[REDACTED]"
            elif normalized in {"displayname", "studentid"}:
                redacted[key] = "[PERSONAL_DATA]"
            else:
                redacted[key] = redact_trace_data(item)
        return redacted
    if isinstance(value, list):
        return [redact_trace_data(item) for item in value]
    if isinstance(value, str):
        return redact_sensitive_text(value)
    return value


class Database:
    """Domain records and audits; vectors/checkpoints are owned by framework adapters."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._pool: asyncpg.Pool | None = None

    @property
    def pool(self) -> asyncpg.Pool:
        if self._pool is None:
            raise RuntimeError("Agent database is not initialized")
        return self._pool

    async def open(self) -> None:
        connection = await asyncpg.connect(self._settings.database_url)
        try:
            await connection.execute("SELECT pg_advisory_lock($1)", 7_104_202_601)
            await connection.execute(
                """
                CREATE TABLE IF NOT EXISTS ai_schema_migration (
                    version VARCHAR(100) PRIMARY KEY,
                    checksum CHAR(64) NOT NULL,
                    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
            )
            await self._migrate(connection)
        finally:
            await connection.execute("SELECT pg_advisory_unlock($1)", 7_104_202_601)
            await connection.close()

        async def initialize_connection(conn: asyncpg.Connection) -> None:
            await conn.set_type_codec(
                "json", schema="pg_catalog", encoder=_json, decoder=json.loads, format="text"
            )
            await conn.set_type_codec(
                "jsonb", schema="pg_catalog", encoder=_json, decoder=json.loads, format="text"
            )

        self._pool = await asyncpg.create_pool(
            self._settings.database_url,
            min_size=self._settings.database_pool_min_size,
            max_size=self._settings.database_pool_max_size,
            init=initialize_connection,
            command_timeout=self._settings.model_timeout_seconds,
        )

    async def _migrate(self, connection: asyncpg.Connection) -> None:
        migration_root = resources.files("app.migrations")
        migrations = sorted(item for item in migration_root.iterdir() if item.name.endswith(".sql"))
        for migration in migrations:
            sql = migration.read_text(encoding="utf-8").replace(
                "{{EMBEDDING_DIMENSIONS}}", str(self._settings.embedding_dimensions)
            )
            checksum = hashlib.sha256(sql.encode("utf-8")).hexdigest()
            existing = await connection.fetchval(
                "SELECT checksum FROM ai_schema_migration WHERE version = $1", migration.name
            )
            if existing and existing != checksum:
                raise RuntimeError(f"Migration checksum changed: {migration.name}")
            if existing:
                continue
            async with connection.transaction():
                await connection.execute(sql)
                await connection.execute(
                    "INSERT INTO ai_schema_migration(version, checksum) VALUES ($1, $2)",
                    migration.name,
                    checksum,
                )

    async def close(self) -> None:
        if self._pool is not None:
            await self._pool.close()
            self._pool = None

    async def ping(self) -> bool:
        try:
            return bool(await self.pool.fetchval("SELECT TRUE"))
        except Exception:  # noqa: BLE001 - readiness deliberately returns one boolean.
            return False

    async def table_exists(self, table_name: str) -> bool:
        return bool(await self.pool.fetchval("SELECT to_regclass($1) IS NOT NULL", table_name))

    async def prepare_knowledge(
        self, request: KnowledgeUpsertRequest, checksum: str
    ) -> tuple[UUID, bool]:
        """Reserve the source URI and return (document_id, already_indexed)."""

        document_id = uuid4()
        async with self.pool.acquire() as connection, connection.transaction():
            await connection.execute(
                "SELECT pg_advisory_xact_lock(hashtextextended($1, 0))", request.sourceUri
            )
            existing = await connection.fetchrow(
                "SELECT id, checksum, status FROM ai_knowledge_document WHERE source_uri=$1 FOR UPDATE",
                request.sourceUri,
            )
            if existing and existing["checksum"] == checksum and existing["status"] == "ACTIVE":
                return existing["id"], True
            if existing:
                document_id = existing["id"]
                await connection.execute(
                    """
                    UPDATE ai_knowledge_document
                       SET title=$2,source_type=$3,checksum=$4,metadata=$5::jsonb,
                           raw_content=$6,status='INDEXING',chunk_count=0,
                           embedding_model=$7,embedding_dimensions=$8,updated_at=NOW()
                     WHERE id=$1
                    """,
                    document_id,
                    request.title,
                    request.sourceType,
                    checksum,
                    request.metadata,
                    redact_sensitive_text(request.content),
                    self._settings.embedding_model,
                    self._settings.embedding_dimensions,
                )
            else:
                await connection.execute(
                    """
                    INSERT INTO ai_knowledge_document(
                        id,title,source_uri,source_type,checksum,status,metadata,raw_content,
                        chunk_count,embedding_model,embedding_dimensions,created_by
                    ) VALUES($1,$2,$3,$4,$5,'INDEXING',$6::jsonb,$7,0,$8,$9,$10)
                    """,
                    document_id,
                    request.title,
                    request.sourceUri,
                    request.sourceType,
                    checksum,
                    request.metadata,
                    redact_sensitive_text(request.content),
                    self._settings.embedding_model,
                    self._settings.embedding_dimensions,
                    request.actorId,
                )
        return document_id, False

    async def complete_knowledge(
        self, document_id: UUID, chunk_count: int
    ) -> KnowledgeDocumentView:
        row = await self.pool.fetchrow(
            """
            UPDATE ai_knowledge_document
               SET status='ACTIVE',chunk_count=$2,indexed_at=NOW(),updated_at=NOW()
             WHERE id=$1
         RETURNING *
            """,
            document_id,
            chunk_count,
        )
        if row is None:
            raise RuntimeError("Knowledge document disappeared during indexing")
        return self._knowledge_view(row)

    async def fail_knowledge(self, document_id: UUID) -> None:
        await self.pool.execute(
            "UPDATE ai_knowledge_document SET status='FAILED',updated_at=NOW() WHERE id=$1",
            document_id,
        )

    async def get_knowledge(self, document_id: UUID) -> KnowledgeDocumentView:
        row = await self.pool.fetchrow(
            "SELECT * FROM ai_knowledge_document WHERE id=$1", document_id
        )
        if row is None:
            raise RuntimeError("Knowledge document not found")
        return self._knowledge_view(row)

    async def list_knowledge(self) -> list[KnowledgeDocumentView]:
        rows = await self.pool.fetch(
            """
            SELECT * FROM ai_knowledge_document
             WHERE status <> 'DELETED'
             ORDER BY updated_at DESC
            """
        )
        return [self._knowledge_view(row) for row in rows]

    async def delete_knowledge(self, document_id: UUID) -> bool:
        result = await self.pool.execute(
            """
            UPDATE ai_knowledge_document
               SET status='DELETED',raw_content='',chunk_count=0,updated_at=NOW()
             WHERE id=$1 AND status <> 'DELETED'
            """,
            document_id,
        )
        return result.endswith("1")

    @staticmethod
    def _knowledge_view(row: asyncpg.Record) -> KnowledgeDocumentView:
        return KnowledgeDocumentView(
            id=row["id"],
            title=row["title"],
            sourceUri=row["source_uri"],
            sourceType=row["source_type"],
            checksum=row["checksum"],
            chunkCount=row["chunk_count"],
            status=row["status"],
            createdAt=row["created_at"],
            updatedAt=row["updated_at"],
        )

    async def start_run(
        self,
        run_id: UUID,
        conversation_id: UUID,
        actor_id: int,
        active_role: str,
        input_text: str,
        model_name: str,
        prompt_version: str,
    ) -> None:
        await self.pool.execute(
            """
            INSERT INTO ai_agent_run(
                run_id,conversation_id,actor_id,active_role,status,input_text,model_name,prompt_version
            ) VALUES($1,$2,$3,$4,'RUNNING',$5,$6,$7)
            ON CONFLICT(run_id) DO NOTHING
            """,
            run_id,
            conversation_id,
            actor_id,
            active_role,
            redact_sensitive_text(input_text),
            model_name,
            prompt_version,
        )

    async def record_tool(
        self,
        run_id: UUID,
        tool_name: str,
        arguments: dict[str, Any],
        result: Any,
        status: str,
        duration_ms: int,
    ) -> None:
        await self.pool.execute(
            """
            INSERT INTO ai_tool_call(id,run_id,tool_name,arguments,result,status,duration_ms)
            VALUES($1,$2,$3,$4::jsonb,$5::jsonb,$6,$7)
            """,
            uuid4(),
            run_id,
            tool_name,
            _bounded_value(redact_trace_data(arguments), 20_000),
            _bounded_value(redact_trace_data(result), 50_000),
            status,
            duration_ms,
        )

    async def finish_run(
        self,
        run_id: UUID,
        status: str,
        output_text: str | None,
        duration_ms: int,
        tool_count: int,
        rag_hit_count: int,
        exception: BaseException | None = None,
    ) -> None:
        await self.pool.execute(
            """
            UPDATE ai_agent_run
               SET status=$2,output_text=$3,finished_at=NOW(),duration_ms=$4,
                   tool_call_count=$5,rag_hit_count=$6,error_type=$7,error_message=$8
             WHERE run_id=$1
            """,
            run_id,
            status,
            redact_sensitive_text(output_text) if output_text else output_text,
            duration_ms,
            tool_count,
            rag_hit_count,
            type(exception).__name__ if exception else None,
            str(exception)[:1000] if exception else None,
        )

    async def save_feedback(self, request: FeedbackRequest) -> bool:
        owner = await self.pool.fetchval(
            "SELECT actor_id FROM ai_agent_run WHERE run_id=$1", request.runId
        )
        if owner != request.actorId:
            return False
        await self.pool.execute(
            """
            INSERT INTO ai_feedback(id,run_id,actor_id,rating,comment)
            VALUES($1,$2,$3,$4,$5)
            ON CONFLICT(run_id,actor_id) DO UPDATE
                SET rating=$4,comment=$5,updated_at=NOW()
            """,
            uuid4(),
            request.runId,
            request.actorId,
            request.rating,
            request.comment,
        )
        return True

    async def quality_summary(self) -> dict[str, Any]:
        row = await self.pool.fetchrow(
            """
            SELECT COUNT(*) FILTER (WHERE started_at >= NOW()-INTERVAL '30 days') AS run_count,
                   COUNT(*) FILTER (WHERE status='SUCCEEDED' AND started_at >= NOW()-INTERVAL '30 days')
                       AS success_count,
                   COALESCE(AVG(duration_ms) FILTER (WHERE status='SUCCEEDED' AND started_at >= NOW()-INTERVAL '30 days'),0)
                       AS avg_duration_ms,
                   COALESCE(AVG(tool_call_count) FILTER (WHERE started_at >= NOW()-INTERVAL '30 days'),0)
                       AS avg_tool_calls,
                   COALESCE(AVG(rag_hit_count) FILTER (WHERE started_at >= NOW()-INTERVAL '30 days'),0)
                       AS avg_rag_hits
              FROM ai_agent_run
            """
        )
        feedback = await self.pool.fetchrow(
            """
            SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE rating=1) AS positive
              FROM ai_feedback WHERE created_at >= NOW()-INTERVAL '30 days'
            """
        )
        run_count = int(row["run_count"])
        feedback_total = int(feedback["total"])
        return {
            "windowDays": 30,
            "runCount": run_count,
            "successRate": float(row["success_count"]) / run_count if run_count else 0.0,
            "averageDurationMs": float(row["avg_duration_ms"]),
            "averageToolCalls": float(row["avg_tool_calls"]),
            "averageRagHits": float(row["avg_rag_hits"]),
            "feedbackCount": feedback_total,
            "positiveRate": float(feedback["positive"]) / feedback_total if feedback_total else 0.0,
        }

    async def cleanup(self, _memory_days: int, trace_days: int) -> dict[str, int]:
        runs = await self.pool.execute(
            "DELETE FROM ai_agent_run WHERE started_at < NOW()-($1::int * INTERVAL '1 day')",
            trace_days,
        )
        return {"runs": int(runs.rsplit(" ", 1)[-1])}
