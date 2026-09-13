from __future__ import annotations

import hashlib
import json
import re
from typing import Any

from app.clients import EmbeddingClient
from app.config import Settings
from app.models import KnowledgeDocumentView, KnowledgeUpsertRequest
from app.storage import Database, redact_sensitive_text


class TextChunker:
    def __init__(self, chunk_chars: int = 1200, overlap_chars: int = 160) -> None:
        self._chunk_chars = chunk_chars
        self._overlap_chars = overlap_chars

    def split(self, content: str) -> list[str]:
        normalized = re.sub(r"\r\n?", "\n", content).strip()
        paragraphs = [part.strip() for part in re.split(r"\n{2,}", normalized) if part.strip()]
        chunks: list[str] = []
        current = ""
        for paragraph in paragraphs:
            candidates = (
                [paragraph]
                if len(paragraph) <= self._chunk_chars
                else [
                    paragraph[index : index + self._chunk_chars]
                    for index in range(0, len(paragraph), self._chunk_chars - self._overlap_chars)
                ]
            )
            for candidate in candidates:
                joined = f"{current}\n\n{candidate}".strip() if current else candidate
                if current and len(joined) > self._chunk_chars:
                    chunks.append(current)
                    prefix = current[-self._overlap_chars :]
                    current = f"{prefix}\n{candidate}".strip()
                else:
                    current = joined
        if current:
            chunks.append(current)
        return [chunk for chunk in chunks if len(chunk) >= 20]


class KnowledgeService:
    def __init__(
        self,
        settings: Settings,
        database: Database,
        embeddings: EmbeddingClient,
        chunker: TextChunker | None = None,
    ) -> None:
        self._settings = settings
        self._database = database
        self._embeddings = embeddings
        self._chunker = chunker or TextChunker()

    async def ingest(self, request: KnowledgeUpsertRequest) -> KnowledgeDocumentView:
        chunks = self._chunker.split(request.content)
        if not chunks:
            raise ValueError("文档切分后没有可入库的有效内容")
        vectors = await self._embeddings.embed(chunks)
        canonical = json.dumps(
            {
                "title": request.title,
                "sourceType": request.sourceType,
                "content": request.content,
                "metadata": request.metadata,
            },
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        )
        checksum = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
        enriched = [
            (
                content,
                max(1, len(content) // 2),
                vectors[index],
                {**request.metadata, "title": request.title, "sourceType": request.sourceType},
            )
            for index, content in enumerate(chunks)
        ]
        return await self._database.upsert_knowledge(request, checksum, enriched)

    async def retrieve(
        self,
        query: str,
        top_k: int | None = None,
        metadata_filter: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        if not query.strip():
            raise ValueError("检索问题不能为空")
        limit = min(max(top_k or self._settings.rag_top_k, 1), 20)
        embedding = (await self._embeddings.embed([query]))[0]
        rows = await self._database.search_knowledge(
            embedding,
            query,
            limit,
            max(limit, self._settings.rag_candidate_k),
            metadata_filter,
        )
        sources = [
            {
                "citation": f"资料{index}",
                "chunkId": str(row["id"]),
                "documentId": str(row["document_id"]),
                "title": row["title"],
                "sourceUri": row["source_uri"],
                "sourceType": row["source_type"],
                "content": row["content"],
                "score": round(float(row["score"]), 4),
                "metadata": row["metadata"],
            }
            for index, row in enumerate(
                [row for row in rows if float(row["score"]) >= self._settings.rag_min_score], 1
            )
        ]
        return {"query": query, "hitCount": len(sources), "sources": sources}


class SemanticMemory:
    def __init__(self, settings: Settings, database: Database, embeddings: EmbeddingClient) -> None:
        self._settings = settings
        self._database = database
        self._embeddings = embeddings

    async def recall(self, actor_id: int, active_role: str, query: str) -> list[dict[str, Any]]:
        embedding = (await self._embeddings.embed([query]))[0]
        return await self._database.recall_memories(
            actor_id, active_role, embedding, self._settings.memory_top_k
        )

    async def remember(
        self,
        actor_id: int,
        conversation_id: Any,
        active_role: str,
        user_message: str,
        assistant_answer: str,
    ) -> None:
        content = redact_sensitive_text(f"用户请求：{user_message}\nAgent答复：{assistant_answer}")
        embedding = (await self._embeddings.embed([content]))[0]
        await self._database.store_memory(
            actor_id,
            conversation_id,
            active_role,
            content,
            embedding,
            self._settings.memory_retention_days,
        )
