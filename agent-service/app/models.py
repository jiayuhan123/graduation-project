from __future__ import annotations

from datetime import datetime
from typing import Any, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class Actor(BaseModel):
    model_config = ConfigDict(extra="forbid")

    userId: int
    displayName: str | None = None
    activeRole: str
    roles: list[str]
    allowedTools: list[str]


class RunRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    runId: UUID
    conversationId: UUID
    message: str = Field(min_length=1, max_length=4000)
    actor: Actor
    toolToken: str = Field(min_length=1)


class ToolInvocation(BaseModel):
    runId: UUID
    toolName: str
    arguments: dict[str, Any]


class JavaResult(BaseModel):
    code: int
    message: str
    data: Any | None = None


class KnowledgeUpsertRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    actorId: int
    title: str = Field(min_length=1, max_length=200)
    sourceUri: str = Field(min_length=1, max_length=1000)
    sourceType: Literal["TEXT", "MARKDOWN", "POLICY", "LESSON", "TEXTBOOK"] = "TEXT"
    content: str = Field(min_length=20, max_length=100_000)
    metadata: dict[str, Any] = Field(default_factory=dict)


class KnowledgeDocumentView(BaseModel):
    id: UUID
    title: str
    sourceUri: str
    sourceType: str
    checksum: str
    chunkCount: int
    status: str
    createdAt: datetime
    updatedAt: datetime


class FeedbackRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    runId: UUID
    actorId: int
    rating: Literal[-1, 1]
    comment: str | None = Field(default=None, max_length=1000)


class ForgetMemoryRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    actorId: int
    conversationId: UUID
