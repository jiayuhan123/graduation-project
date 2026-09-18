from __future__ import annotations

from dataclasses import dataclass
from typing import Any
from uuid import uuid4

import pytest

from app.config import Settings
from app.memory import AgentMemory


@dataclass
class Item:
    key: str
    value: dict[str, Any]
    score: float | None = 0.9


class FakeStore:
    def __init__(self) -> None:
        self.values: dict[tuple[tuple[str, ...], str], dict[str, Any]] = {}

    async def aput(
        self, namespace: tuple[str, ...], key: str, value: dict[str, Any], **_kwargs: Any
    ) -> None:
        self.values[(namespace, key)] = value

    async def asearch(
        self,
        namespace: tuple[str, ...],
        *,
        query: str | None = None,
        filter: dict[str, Any] | None = None,
        **_kwargs: Any,
    ) -> list[Item]:
        del query
        return [
            Item(key, value)
            for (item_namespace, key), value in self.values.items()
            if item_namespace == namespace
            and (
                not filter or all(value.get(name) == expected for name, expected in filter.items())
            )
        ]

    async def adelete(self, namespace: tuple[str, ...], key: str) -> None:
        self.values.pop((namespace, key), None)

    async def alist_namespaces(self, **_kwargs: Any) -> list[tuple[str, ...]]:
        return list(dict.fromkeys(namespace for namespace, _key in self.values))


class FakeCheckpointer:
    def __init__(self) -> None:
        self.deleted: list[str] = []

    async def adelete_thread(self, thread_id: str) -> None:
        self.deleted.append(thread_id)


def settings() -> Settings:
    return Settings("b", "http://java", "i", "http://model", "k", "m", 5, 4)


@pytest.mark.asyncio
async def test_memory_is_isolated_by_actor_and_role_and_can_be_forgotten() -> None:
    store = FakeStore()
    checkpointer = FakeCheckpointer()
    memory = AgentMemory(settings(), store, checkpointer)  # type: ignore[arg-type]
    conversation_id = uuid4()

    await memory.remember(uuid4(), 7, conversation_id, "teacher", "分析班级", "建议复习")
    assert len(await memory.recall(7, "teacher", "复习")) == 1
    assert await memory.recall(7, "admin", "复习") == []

    deleted = await memory.forget(7, conversation_id)

    assert deleted == 1
    assert await memory.recall(7, "teacher", "复习") == []
    assert set(checkpointer.deleted) == {
        f"7:{conversation_id}:teacher",
        f"7:{conversation_id}:admin",
    }
