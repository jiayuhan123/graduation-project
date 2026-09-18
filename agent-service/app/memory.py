from __future__ import annotations

from typing import Any
from uuid import UUID

from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.store.postgres.aio import AsyncPostgresStore

from app.config import Settings
from app.storage import redact_sensitive_text


class AgentMemory:
    """LangGraph checkpointed short-term memory plus semantic PostgreSQL memory."""

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
        return f"{actor_id}:{conversation_id}:{active_role}"

    @staticmethod
    def namespace(actor_id: int, active_role: str) -> tuple[str, ...]:
        return ("agent_memory", str(actor_id), active_role)

    async def recall(self, actor_id: int, active_role: str, query: str) -> list[dict[str, Any]]:
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
        conversation = str(conversation_id)
        deleted = 0
        # A conversation may have checkpoints/memories under more than one switched role.
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
