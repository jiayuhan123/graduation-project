\set ON_ERROR_STOP on

DO $$
DECLARE
    ai_table_count INTEGER;
    chunk_embedding_type TEXT;
    memory_embedding_type TEXT;
    hnsw_index_count INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO ai_table_count
      FROM pg_tables
     WHERE schemaname = 'public'
       AND tablename LIKE 'ai_%';

    IF ai_table_count <> 8 THEN
        RAISE EXCEPTION 'Expected 8 AI tables, found %', ai_table_count;
    END IF;

    SELECT format_type(attribute.atttypid, attribute.atttypmod)
      INTO chunk_embedding_type
      FROM pg_attribute attribute
      JOIN pg_class relation ON relation.oid = attribute.attrelid
     WHERE relation.relname = 'ai_knowledge_chunk'
       AND attribute.attname = 'embedding'
       AND attribute.attnum > 0
       AND NOT attribute.attisdropped;

    SELECT format_type(attribute.atttypid, attribute.atttypmod)
      INTO memory_embedding_type
      FROM pg_attribute attribute
      JOIN pg_class relation ON relation.oid = attribute.attrelid
     WHERE relation.relname = 'ai_memory'
       AND attribute.attname = 'embedding'
       AND attribute.attnum > 0
       AND NOT attribute.attisdropped;

    IF chunk_embedding_type <> 'vector(1024)'
       OR memory_embedding_type <> 'vector(1024)' THEN
        RAISE EXCEPTION 'Unexpected embedding types: chunk=%, memory=%',
            chunk_embedding_type, memory_embedding_type;
    END IF;

    SELECT COUNT(*)
      INTO hnsw_index_count
      FROM pg_indexes
     WHERE schemaname = 'public'
       AND indexname IN (
           'idx_ai_knowledge_chunk_embedding',
           'idx_ai_memory_embedding'
       )
       AND indexdef ILIKE '%USING hnsw%';

    IF hnsw_index_count <> 2 THEN
        RAISE EXCEPTION 'Expected 2 HNSW indexes, found %', hnsw_index_count;
    END IF;
END
$$;

SELECT extversion AS vector_version
FROM pg_extension
WHERE extname = 'vector';

SELECT tablename
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename LIKE 'ai_%'
ORDER BY tablename;

SELECT
    format_type(attribute.atttypid, attribute.atttypmod) AS embedding_type
FROM pg_attribute attribute
JOIN pg_class relation ON relation.oid = attribute.attrelid
WHERE relation.relname IN ('ai_knowledge_chunk', 'ai_memory')
  AND attribute.attname = 'embedding'
  AND attribute.attnum > 0
  AND NOT attribute.attisdropped
ORDER BY relation.relname;
