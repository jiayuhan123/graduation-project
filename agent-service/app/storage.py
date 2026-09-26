"""项目自有 PostgreSQL 数据、迁移和审计访问层。

本模块使用 asyncpg 管理知识文档主记录、Agent 运行、工具调用和反馈，并在数据落库前执行
基础脱敏。向量 chunk、LangGraph checkpoint 和长期 Store 由各自官方适配器管理，本类只
管理项目能够稳定定义和查询的领域表，避免绕过框架直接操作其内部结构。
"""

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
    """把 Python 值序列化为紧凑 JSON，保留中文并兼容 UUID 等对象。"""

    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), default=str)


def _bounded_value(value: Any, limit: int) -> Any:
    """限制审计 JSON 体积；超限时只保存明确标记的预览。"""

    encoded = _json(value)
    if len(encoded) <= limit:
        return value
    return {"truncated": True, "preview": encoded[: limit - 50]}


def redact_sensitive_text(value: str) -> str:
    """尽力移除被用户误贴进轨迹、回答或记忆的常见凭证。"""

    redacted = _SECRET_ASSIGNMENT.sub(r"\1\2[REDACTED]", value)
    redacted = _CHINESE_SECRET_ASSIGNMENT.sub(r"\1\2[REDACTED]", redacted)
    redacted = _BEARER_TOKEN.sub("Bearer [REDACTED]", redacted)
    return _OPENAI_STYLE_KEY.sub("[REDACTED]", redacted)


def redact_trace_data(value: Any) -> Any:
    """递归脱敏工具参数/结果中的凭证键、学生 ID、显示名和文本。"""

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
    """管理领域记录与审计；向量和 checkpoint 由框架适配器负责。"""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._pool: asyncpg.Pool | None = None

    @property
    def pool(self) -> asyncpg.Pool:
        """返回已初始化连接池；在 lifespan 启动完成前调用属于编程错误。"""

        if self._pool is None:
            raise RuntimeError("Agent database is not initialized")
        return self._pool

    async def open(self) -> None:
        """串行执行版本化迁移，然后创建供请求共享的 asyncpg 连接池。"""

        connection = await asyncpg.connect(self._settings.database_url)
        try:
            # 固定 advisory lock 让多个副本启动时只有一个实例执行项目迁移。
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
            """让 asyncpg 自动在 Python 对象与 PostgreSQL JSON/JSONB 间转换。"""

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
        """按文件名顺序执行未应用迁移，并拒绝被修改的历史迁移。"""

        migration_root = resources.files("app.migrations")
        migrations = sorted(item for item in migration_root.iterdir() if item.name.endswith(".sql"))
        for migration in migrations:
            # 旧版迁移使用维度占位符；checksum 对替换后的真实 SQL 计算。
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
            # SQL 与迁移历史必须在同一事务提交，避免只执行一半却被标记成功。
            async with connection.transaction():
                await connection.execute(sql)
                await connection.execute(
                    "INSERT INTO ai_schema_migration(version, checksum) VALUES ($1, $2)",
                    migration.name,
                    checksum,
                )

    async def close(self) -> None:
        """关闭请求连接池；方法可重复调用。"""

        if self._pool is not None:
            await self._pool.close()
            self._pool = None

    async def ping(self) -> bool:
        """执行最轻量查询并把任何异常折叠成 readiness 所需布尔值。"""

        try:
            return bool(await self.pool.fetchval("SELECT TRUE"))
        except Exception:  # noqa: BLE001 - readiness deliberately returns one boolean.
            return False

    async def table_exists(self, table_name: str) -> bool:
        """通过 PostgreSQL catalog 判断关系表是否存在。"""

        return bool(await self.pool.fetchval("SELECT to_regclass($1) IS NOT NULL", table_name))

    async def prepare_knowledge(
        self, request: KnowledgeUpsertRequest, checksum: str
    ) -> tuple[UUID, bool]:
        """锁定来源 URI，准备入库并返回 `(文档ID, 是否已完成相同内容)`。"""

        document_id = uuid4()
        async with self.pool.acquire() as connection, connection.transaction():
            # 对 URI 哈希加事务级锁，避免并发请求为同一来源重复做昂贵 Embedding。
            await connection.execute(
                "SELECT pg_advisory_xact_lock(hashtextextended($1, 0))", request.sourceUri
            )
            existing = await connection.fetchrow(
                "SELECT id, checksum, status FROM ai_knowledge_document WHERE source_uri=$1 FOR UPDATE",
                request.sourceUri,
            )
            # 内容未变化且旧索引可用时直接复用，实现幂等入库。
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
        """向量写入成功后把主记录切换为 ACTIVE 并返回对外视图。"""

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
        """向量入库失败后保留主记录并标记 FAILED，方便诊断或重试。"""

        await self.pool.execute(
            "UPDATE ai_knowledge_document SET status='FAILED',updated_at=NOW() WHERE id=$1",
            document_id,
        )

    async def get_knowledge(self, document_id: UUID) -> KnowledgeDocumentView:
        """按 ID 读取一条知识文档摘要。"""

        row = await self.pool.fetchrow(
            "SELECT * FROM ai_knowledge_document WHERE id=$1", document_id
        )
        if row is None:
            raise RuntimeError("Knowledge document not found")
        return self._knowledge_view(row)

    async def list_knowledge(self) -> list[KnowledgeDocumentView]:
        """按最近更新时间倒序列出所有未软删除的知识文档。"""

        rows = await self.pool.fetch(
            """
            SELECT * FROM ai_knowledge_document
             WHERE status <> 'DELETED'
             ORDER BY updated_at DESC
            """
        )
        return [self._knowledge_view(row) for row in rows]

    async def delete_knowledge(self, document_id: UUID) -> bool:
        """软删除文档主记录并清空原文，返回本次是否实际删除。"""

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
        """把数据库 snake_case 行映射为 API camelCase Pydantic 视图。"""

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
        """幂等记录一次 Agent 运行开始，并在入库前脱敏用户输入。"""

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
        """保存一次工具调用的脱敏参数、结果、状态与耗时。"""

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
        """结束运行并写入最终状态、输出、统计以及可选异常摘要。"""

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
        """仅允许运行所有者新增或覆盖反馈；run 不匹配时返回 False。"""

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
        """用 PostgreSQL 条件聚合生成最近 30 天质量概览。"""

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
        # 分母为 0 时显式返回 0，避免除零并让接口字段保持稳定。
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
        """删除超过审计保留期的运行；工具调用和反馈通过外键级联删除。

        长期记忆由 LangGraph Store TTL sweeper 清理，因此 `_memory_days` 只保留接口语义，
        当前方法不直接操作 Store 的框架表。
        """

        runs = await self.pool.execute(
            "DELETE FROM ai_agent_run WHERE started_at < NOW()-($1::int * INTERVAL '1 day')",
            trace_days,
        )
        return {"runs": int(runs.rsplit(" ", 1)[-1])}
