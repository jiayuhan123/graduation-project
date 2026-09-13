CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    source_uri VARCHAR(1000) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    checksum CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_knowledge_source UNIQUE (source_uri)
);

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES ai_knowledge_document(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_tsv TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    token_count INTEGER NOT NULL,
    embedding VECTOR({{EMBEDDING_DIMENSIONS}}) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_knowledge_chunk UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_document ON ai_knowledge_chunk(document_id);
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_tsv ON ai_knowledge_chunk USING GIN(content_tsv);
CREATE INDEX IF NOT EXISTS idx_ai_knowledge_chunk_embedding
    ON ai_knowledge_chunk USING HNSW (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS ai_conversation (
    id UUID NOT NULL,
    actor_id BIGINT NOT NULL,
    active_role VARCHAR(32) NOT NULL,
    summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, actor_id, active_role)
);

CREATE TABLE IF NOT EXISTS ai_memory (
    id UUID PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    conversation_id UUID NOT NULL,
    active_role VARCHAR(32) NOT NULL,
    memory_type VARCHAR(24) NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR({{EMBEDDING_DIMENSIONS}}) NOT NULL,
    importance NUMERIC(4,3) NOT NULL DEFAULT 0.500,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_memory_conversation
        FOREIGN KEY (conversation_id, actor_id, active_role)
        REFERENCES ai_conversation(id, actor_id, active_role) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_memory_actor_role_expiry
    ON ai_memory(actor_id, active_role, expires_at);
CREATE INDEX IF NOT EXISTS idx_ai_memory_embedding
    ON ai_memory USING HNSW (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS ai_agent_run (
    run_id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    actor_id BIGINT NOT NULL,
    active_role VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    input_text TEXT NOT NULL,
    output_text TEXT,
    model_name VARCHAR(200),
    prompt_version VARCHAR(64) NOT NULL,
    tool_call_count INTEGER NOT NULL DEFAULT 0,
    rag_hit_count INTEGER NOT NULL DEFAULT 0,
    error_type VARCHAR(200),
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    duration_ms BIGINT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_ai_agent_run_actor_time ON ai_agent_run(actor_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_agent_run_status_time ON ai_agent_run(status, started_at DESC);

CREATE TABLE IF NOT EXISTS ai_tool_call (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES ai_agent_run(run_id) ON DELETE CASCADE,
    tool_name VARCHAR(64) NOT NULL,
    arguments JSONB NOT NULL,
    result JSONB,
    status VARCHAR(16) NOT NULL,
    duration_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_tool_call_run ON ai_tool_call(run_id, created_at);

CREATE TABLE IF NOT EXISTS ai_feedback (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES ai_agent_run(run_id) ON DELETE CASCADE,
    actor_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating IN (-1, 1)),
    comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_feedback_actor_run UNIQUE (run_id, actor_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_feedback_time ON ai_feedback(created_at DESC);
