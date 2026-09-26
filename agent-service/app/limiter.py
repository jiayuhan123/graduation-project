"""Agent 请求的频率限制与并发保护。

Redis 计数器限制单个用户每分钟的请求数，可在多个服务实例间共享；`asyncio.Semaphore`
限制当前 Python 进程同时运行的 Agent 数，防止模型、数据库和 Java 工具被瞬时流量压垮。
"""

from __future__ import annotations

import asyncio
import time

from redis.asyncio import Redis


class RunLimiter:
    """组合“分布式用户限流”和“单进程总并发限制”。"""

    def __init__(self, redis: Redis, per_minute: int, max_concurrent: int) -> None:
        self._redis = redis
        self._per_minute = per_minute
        self._semaphore = asyncio.Semaphore(max_concurrent)

    async def check_rate(self, actor_id: int) -> None:
        """增加当前用户的分钟窗口计数，超过阈值时拒绝本次请求。"""

        window = int(time.time() // 60)
        key = f"ai:rate:{actor_id}:{window}"
        # transaction=True 让 INCR 和 EXPIRE 在一个 Redis 事务中提交，避免只有计数没有过期时间。
        async with self._redis.pipeline(transaction=True) as pipe:
            pipe.incr(key)
            pipe.expire(key, 120)
            count, _ = await pipe.execute()
        if int(count) > self._per_minute:
            raise RuntimeError("Agent 请求过于频繁，请稍后重试")

    async def __aenter__(self) -> None:
        """进入 `async with` 时等待一个进程内并发槽位。"""

        await self._semaphore.acquire()

    async def __aexit__(self, exc_type: object, exc: object, traceback: object) -> None:
        """无论运行成功、失败或取消，都归还并发槽位。"""

        self._semaphore.release()
