from __future__ import annotations

import asyncio
import time

from redis.asyncio import Redis


class RunLimiter:
    def __init__(self, redis: Redis, per_minute: int, max_concurrent: int) -> None:
        self._redis = redis
        self._per_minute = per_minute
        self._semaphore = asyncio.Semaphore(max_concurrent)

    async def check_rate(self, actor_id: int) -> None:
        window = int(time.time() // 60)
        key = f"ai:rate:{actor_id}:{window}"
        async with self._redis.pipeline(transaction=True) as pipe:
            pipe.incr(key)
            pipe.expire(key, 120)
            count, _ = await pipe.execute()
        if int(count) > self._per_minute:
            raise RuntimeError("Agent 请求过于频繁，请稍后重试")

    async def __aenter__(self) -> None:
        await self._semaphore.acquire()

    async def __aexit__(self, exc_type: object, exc: object, traceback: object) -> None:
        self._semaphore.release()
