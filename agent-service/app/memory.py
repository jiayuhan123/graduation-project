"""封装 LangGraph 的短期 checkpoint 与长期语义记忆。

Checkpoint 保存一条会话线程的图状态和消息，Store 保存可跨会话召回的用户经验。二者都在
PostgreSQL 中，但隔离键、数据结构和生命周期不同，不应混为普通聊天记录。
"""

from __future__ import annotations

from typing import Any
from uuid import UUID

from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.store.postgres.aio import AsyncPostgresStore

from app.config import Settings
from app.storage import redact_sensitive_text


class AgentMemory:
    """为业务层提供 checkpoint 线程键、长期记忆召回、写入和遗忘能力。"""

    def __init__(
        self,
        settings: Settings,
        store: AsyncPostgresStore,
        checkpointer: AsyncPostgresSaver,
    ) -> None:
        self._settings = settings
        self.store = store
        self.checkpointer = checkpointer

    @staticmethod
    def thread_id(actor_id: int, conversation_id: UUID, active_role: str) -> str:
        """生成 checkpoint 隔离键：用户、会话或角色任一不同都会进入不同线程。"""

        return f"{actor_id}:{conversation_id}:{active_role}"

    @staticmethod
    def namespace(actor_id: int, active_role: str) -> tuple[str, ...]:
        """生成长期记忆 namespace；故意不含会话 ID，以支持同角色跨会话召回。"""

        return ("agent_memory", str(actor_id), active_role)

    async def recall(self, actor_id: int, active_role: str, query: str) -> list[dict[str, Any]]:
        """按用户和当前角色对历史记忆做向量语义搜索。"""

        items = await self.store.asearch(
            self.namespace(actor_id, active_role),
            query=query,
            limit=self._settings.memory_top_k,
            refresh_ttl=True,
        )
        return [
            {
                **item.value,
                "key": item.key,
                "score": item.score,
            }
            for item in items
        ]

    async def remember(
        self,
        run_id: UUID,
        actor_id: int,
        conversation_id: UUID,
        active_role: str,
        user_message: str,
        assistant_answer: str,
    ) -> None:
        """把一轮成功问答保存为有 TTL 的 EPISODIC 长期记忆。"""

        # 记忆将来会重新进入模型上下文，写入前先脱敏并限制长度，降低泄密和上下文膨胀风险。
        content = redact_sensitive_text(f"用户请求：{user_message}\nAgent答复：{assistant_answer}")[
            :6000
        ]
        await self.store.aput(
            self.namespace(actor_id, active_role),
            str(run_id),
            {
                "content": content,
                "conversationId": str(conversation_id),
                "runId": str(run_id),
                "memoryType": "EPISODIC",
            },
            index=["content"],
            ttl=float(self._settings.memory_retention_days * 24 * 60),
        )

    async def forget(self, actor_id: int, conversation_id: UUID) -> int:
        """删除指定会话在所有角色下的长期记忆和 checkpoint，返回记忆删除数量。"""

        conversation = str(conversation_id)
        deleted = 0
        # 同一 conversation 可能发生角色切换，所以既枚举实际 namespace，也覆盖默认角色。
        namespaces = await self.store.alist_namespaces(
            prefix=("agent_memory", str(actor_id)), max_depth=3, limit=100
        )
        roles = {"teacher", "admin"}
        roles.update(namespace[2] for namespace in namespaces if len(namespace) >= 3)
        for role in roles:
            namespace = self.namespace(actor_id, role)
            items = await self.store.asearch(
                namespace,
                filter={"conversationId": conversation},
                limit=1000,
            )
            for item in items:
                await self.store.adelete(namespace, item.key)
                deleted += 1
            await self.checkpointer.adelete_thread(self.thread_id(actor_id, conversation_id, role))
        return deleted
