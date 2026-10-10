from typing import Any

import pytest

from app.config import Settings
from app.storage import Database, redact_sensitive_text, redact_trace_data


class CommentPool:
    """只捕获 COMMENT SQL 的最小 asyncpg Pool 测试替身。"""

    def __init__(self) -> None:
        self.executed_sql = ""

    async def execute(self, sql: str, *_args: Any) -> str:
        self.executed_sql = sql
        return "OK"


def test_trace_redaction_removes_tokens_and_student_identity() -> None:
    value = {
        "confirmationToken": "signed-action",
        "alerts": [{"studentId": 8, "displayName": "小明", "severity": "HIGH"}],
    }

    redacted = redact_trace_data(value)

    assert redacted["confirmationToken"] == "[REDACTED]"
    assert redacted["alerts"][0]["studentId"] == "[PERSONAL_DATA]"
    assert redacted["alerts"][0]["displayName"] == "[PERSONAL_DATA]"
    assert redacted["alerts"][0]["severity"] == "HIGH"


def test_sensitive_text_redaction_covers_common_credential_formats() -> None:
    value = "api_key=abc123456789 password: hunter2 Bearer abcdefghijklmnop sk-live_secret123"

    redacted = redact_sensitive_text(value)

    assert "abc123456789" not in redacted
    assert "hunter2" not in redacted
    assert "abcdefghijklmnop" not in redacted
    assert "sk-live_secret123" not in redacted
    assert redacted.count("[REDACTED]") == 4


@pytest.mark.asyncio
async def test_schema_comments_cover_all_current_agent_tables() -> None:
    settings = Settings("bridge", "http://java", "internal", "http://model", "key", "model", 5, 4)
    database = Database(settings)
    pool = CommentPool()
    database._pool = pool  # type: ignore[assignment]  # 测试不连接真实 PostgreSQL。

    await database.apply_schema_comments()

    for table in (
        "ai_agent_run",
        "ai_feedback",
        "ai_knowledge_chunk",
        "ai_knowledge_document",
        "ai_schema_migration",
        "ai_tool_call",
        "checkpoints",
        "checkpoint_blobs",
        "checkpoint_migrations",
        "checkpoint_writes",
        "store",
        "store_migrations",
        "store_vectors",
        "vector_migrations",
    ):
        assert f"COMMENT ON TABLE public.{table}" in pool.executed_sql
