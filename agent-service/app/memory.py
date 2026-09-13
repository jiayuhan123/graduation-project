from __future__ import annotations

import asyncio
import json
from collections import defaultdict
from typing import Any
from uuid import UUID

from redis.asyncio import Redis


class ConversationMemory:
    """Process-local bounded memory; production can replace this port with Redis."""

    def __init__(self, max_messages: int = 12) -> None:
        self._max_messages = max_messages
        self._messages: dict[tuple[int, UUID, str], list[dict[str, Any]]] = defaultdict(list)
        self._lock = asyncio.Lock()

    async def get(
        self, actor_id: int, conversation_id: UUID, active_role: str = "default"
    ) -> list[dict[str, Any]]:
        async with self._lock:
            return [
                dict(message)
                for message in self._messages[(actor_id, conversation_id, active_role)]
            ]

    async def append(
        self,
        actor_id: int,
        conversation_id: UUID,
        messages: list[dict[str, Any]],
        active_role: str = "default",
    ) -> None:
        async with self._lock:
            history = self._messages[(actor_id, conversation_id, active_role)]
            history.extend(messages)
            del history[: max(0, len(history) - self._max_messages)]

    async def forget(self, actor_id: int, conversation_id: UUID) -> None:
        async with self._lock:
            for key in [
                key for key in self._messages if key[0] == actor_id and key[1] == conversation_id
            ]:
                self._messages.pop(key, None)


class RedisConversationMemory:
    """Redis-backed bounded short-term memory with actor isolation and expiry."""

    def __init__(self, client: Redis, max_messages: int, ttl_seconds: int) -> None:
        self._client = client
        self._max_messages = max_messages
        self._ttl_seconds = ttl_seconds

    def _key(self, actor_id: int, conversation_id: UUID, active_role: str) -> str:
        return f"ai:conversation:{actor_id}:{conversation_id}:{active_role}"

    async def get(
        self, actor_id: int, conversation_id: UUID, active_role: str = "default"
    ) -> list[dict[str, Any]]:
        values = await self._client.lrange(self._key(actor_id, conversation_id, active_role), 0, -1)
        return [json.loads(value) for value in values]

    async def append(
        self,
        actor_id: int,
        conversation_id: UUID,
        messages: list[dict[str, Any]],
        active_role: str = "default",
    ) -> None:
        if not messages:
            return
        key = self._key(actor_id, conversation_id, active_role)
        encoded = [
            json.dumps(message, ensure_ascii=False, separators=(",", ":")) for message in messages
        ]
        async with self._client.pipeline(transaction=True) as pipe:
            pipe.rpush(key, *encoded)
            pipe.ltrim(key, -self._max_messages, -1)
            pipe.expire(key, self._ttl_seconds)
            await pipe.execute()

    async def forget(self, actor_id: int, conversation_id: UUID) -> None:
        pattern = f"ai:conversation:{actor_id}:{conversation_id}:*"
        keys = [key async for key in self._client.scan_iter(match=pattern, count=20)]
        if keys:
            await self._client.delete(*keys)
