from uuid import uuid4

import pytest

from app.memory import ConversationMemory


@pytest.mark.asyncio
async def test_short_term_memory_is_isolated_by_active_role() -> None:
    memory = ConversationMemory()
    conversation_id = uuid4()
    await memory.append(7, conversation_id, [{"role": "user", "content": "admin"}], "admin")
    await memory.append(7, conversation_id, [{"role": "user", "content": "teacher"}], "teacher")

    assert (await memory.get(7, conversation_id, "admin"))[0]["content"] == "admin"
    assert (await memory.get(7, conversation_id, "teacher"))[0]["content"] == "teacher"

    await memory.forget(7, conversation_id)
    assert await memory.get(7, conversation_id, "admin") == []
    assert await memory.get(7, conversation_id, "teacher") == []
