from __future__ import annotations

import os
from dataclasses import dataclass
from urllib.parse import quote


def _integer(name: str, default: int) -> int:
    value = os.getenv(name)
    return int(value) if value else default


def _float(name: str, default: float) -> float:
    value = os.getenv(name)
    return float(value) if value else default


def _boolean(name: str, default: bool) -> bool:
    value = os.getenv(name)
    return value.lower() in {"1", "true", "yes", "on"} if value else default


def _database_url() -> str:
    explicit = os.getenv("AI_DATABASE_URL")
    if explicit:
        return explicit
    username = quote(os.getenv("AI_DB_USERNAME", os.getenv("DB_USERNAME", "postgres")), safe="")
    password = quote(os.getenv("AI_DB_PASSWORD", os.getenv("DB_PASSWORD", "postgres")), safe="")
    host = os.getenv("AI_DB_HOST", os.getenv("DB_HOST", "postgres"))
    port = _integer("AI_DB_PORT", _integer("POSTGRES_PORT", 5432))
    database = quote(os.getenv("AI_DB_NAME", os.getenv("MONOLITH_DB_NAME", "monolith_db")), safe="")
    return f"postgresql://{username}:{password}@{host}:{port}/{database}"


def _redis_url() -> str:
    explicit = os.getenv("AI_REDIS_URL")
    if explicit:
        return explicit
    password = quote(os.getenv("AI_REDIS_PASSWORD", os.getenv("REDIS_PASSWORD", "")), safe="")
    host = os.getenv("AI_REDIS_HOST", os.getenv("REDIS_HOST", "redis"))
    port = _integer("AI_REDIS_PORT", _integer("REDIS_PORT", 6379))
    database = _integer("AI_REDIS_DATABASE", _integer("REDIS_DATABASE", 0))
    credentials = f":{password}@" if password else ""
    return f"redis://{credentials}{host}:{port}/{database}"


@dataclass(frozen=True, slots=True)
class Settings:
    bridge_token: str
    java_tool_base_url: str
    java_internal_api_token: str
    model_base_url: str
    model_api_key: str
    model_name: str
    model_timeout_seconds: int
    max_tool_rounds: int
    embedding_model: str = ""
    embedding_dimensions: int = 1024
    embedding_batch_size: int = 20
    database_url: str = "postgresql://postgres:postgres@postgres:5432/monolith_db"
    database_pool_min_size: int = 2
    database_pool_max_size: int = 10
    redis_url: str = "redis://redis:6379/0"
    conversation_memory_ttl_seconds: int = 604800
    conversation_max_messages: int = 20
    context_history_chars: int = 24000
    rag_top_k: int = 6
    rag_candidate_k: int = 24
    rag_min_score: float = 0.5
    memory_top_k: int = 4
    memory_retention_days: int = 180
    trace_retention_days: int = 90
    max_requests_per_minute: int = 20
    max_concurrent_runs: int = 32
    docs_enabled: bool = False

    @classmethod
    def from_environment(cls) -> Settings:
        return cls(
            bridge_token=os.getenv("AI_AGENT_BRIDGE_TOKEN", ""),
            java_tool_base_url=os.getenv(
                "JAVA_AI_TOOL_BASE_URL", "http://host.docker.internal:10000"
            ).rstrip("/"),
            java_internal_api_token=os.getenv("INTERNAL_API_TOKEN", ""),
            model_base_url=os.getenv("AI_MODEL_BASE_URL", "https://api.openai.com/v1").rstrip("/"),
            model_api_key=os.getenv("AI_MODEL_API_KEY", ""),
            model_name=os.getenv("AI_MODEL_NAME", ""),
            model_timeout_seconds=_integer("AI_MODEL_TIMEOUT_SECONDS", 60),
            max_tool_rounds=_integer("AI_MAX_TOOL_ROUNDS", 4),
            embedding_model=os.getenv("AI_EMBEDDING_MODEL", ""),
            embedding_dimensions=_integer("AI_EMBEDDING_DIMENSIONS", 1024),
            embedding_batch_size=_integer("AI_EMBEDDING_BATCH_SIZE", 20),
            database_url=_database_url(),
            database_pool_min_size=_integer("AI_DATABASE_POOL_MIN_SIZE", 2),
            database_pool_max_size=_integer("AI_DATABASE_POOL_MAX_SIZE", 10),
            redis_url=_redis_url(),
            conversation_memory_ttl_seconds=_integer("AI_CONVERSATION_MEMORY_TTL_SECONDS", 604800),
            conversation_max_messages=_integer("AI_CONVERSATION_MAX_MESSAGES", 20),
            context_history_chars=_integer("AI_CONTEXT_HISTORY_CHARS", 24000),
            rag_top_k=_integer("AI_RAG_TOP_K", 6),
            rag_candidate_k=_integer("AI_RAG_CANDIDATE_K", 24),
            rag_min_score=_float("AI_RAG_MIN_SCORE", 0.5),
            memory_top_k=_integer("AI_MEMORY_TOP_K", 4),
            memory_retention_days=_integer("AI_MEMORY_RETENTION_DAYS", 180),
            trace_retention_days=_integer("AI_TRACE_RETENTION_DAYS", 90),
            max_requests_per_minute=_integer("AI_MAX_REQUESTS_PER_MINUTE", 20),
            max_concurrent_runs=_integer("AI_MAX_CONCURRENT_RUNS", 32),
            docs_enabled=_boolean("AI_DOCS_ENABLED", False),
        )

    @property
    def model_configured(self) -> bool:
        return bool(self.model_api_key and self.model_name)

    @property
    def embedding_configured(self) -> bool:
        return bool(self.model_api_key and self.embedding_model)

    @property
    def credentials_configured(self) -> bool:
        return len(self.bridge_token) >= 24 and len(self.java_internal_api_token) >= 24

    def validate(self) -> None:
        if self.embedding_dimensions < 64 or self.embedding_dimensions > 2000:
            raise ValueError("AI_EMBEDDING_DIMENSIONS must be between 64 and 2000")
        if self.database_pool_min_size < 1:
            raise ValueError("AI_DATABASE_POOL_MIN_SIZE must be positive")
        if self.database_pool_max_size < self.database_pool_min_size:
            raise ValueError("AI_DATABASE_POOL_MAX_SIZE must be >= AI_DATABASE_POOL_MIN_SIZE")
        if self.model_timeout_seconds < 1 or self.model_timeout_seconds > 300:
            raise ValueError("AI_MODEL_TIMEOUT_SECONDS must be between 1 and 300")
        if self.max_tool_rounds < 1 or self.max_tool_rounds > 8:
            raise ValueError("AI_MAX_TOOL_ROUNDS must be between 1 and 8")
        if self.conversation_memory_ttl_seconds < 60:
            raise ValueError("AI_CONVERSATION_MEMORY_TTL_SECONDS must be at least 60")
        if self.conversation_max_messages < 2 or self.conversation_max_messages > 100:
            raise ValueError("AI_CONVERSATION_MAX_MESSAGES must be between 2 and 100")
        if self.rag_top_k < 1 or self.rag_top_k > 20:
            raise ValueError("AI_RAG_TOP_K must be between 1 and 20")
        if self.rag_candidate_k < self.rag_top_k or self.rag_candidate_k > 200:
            raise ValueError("AI_RAG_CANDIDATE_K must be >= top-k and <= 200")
        if self.embedding_batch_size < 1 or self.embedding_batch_size > 256:
            raise ValueError("AI_EMBEDDING_BATCH_SIZE must be between 1 and 256")
        if self.rag_min_score < 0 or self.rag_min_score > 1:
            raise ValueError("AI_RAG_MIN_SCORE must be between 0 and 1")
        if self.max_concurrent_runs < 1:
            raise ValueError("AI_MAX_CONCURRENT_RUNS must be positive")
        if self.memory_top_k < 1 or self.memory_top_k > 10:
            raise ValueError("AI_MEMORY_TOP_K must be between 1 and 10")
        if self.memory_retention_days < 1 or self.memory_retention_days > 3650:
            raise ValueError("AI_MEMORY_RETENTION_DAYS must be between 1 and 3650")
        if self.trace_retention_days < 1 or self.trace_retention_days > 3650:
            raise ValueError("AI_TRACE_RETENTION_DAYS must be between 1 and 3650")
        if self.max_requests_per_minute < 1:
            raise ValueError("AI_MAX_REQUESTS_PER_MINUTE must be positive")
        if self.context_history_chars < 4000 or self.context_history_chars > 200000:
            raise ValueError("AI_CONTEXT_HISTORY_CHARS must be between 4000 and 200000")
