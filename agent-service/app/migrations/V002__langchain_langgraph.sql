-- The user chose a clean replacement: old hand-written RAG and memory data is discarded.
DROP TABLE IF EXISTS ai_memory CASCADE;
DROP TABLE IF EXISTS ai_conversation CASCADE;
DROP TABLE IF EXISTS ai_knowledge_chunk CASCADE;

TRUNCATE TABLE ai_knowledge_document;

ALTER TABLE ai_knowledge_document
    ADD COLUMN IF NOT EXISTS raw_content TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS chunk_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(200),
    ADD COLUMN IF NOT EXISTS embedding_dimensions INTEGER,
    ADD COLUMN IF NOT EXISTS indexed_at TIMESTAMPTZ;
