"""Java、FastAPI 路由和 Agent 内部共同使用的数据协议。

这里的 Pydantic 模型相当于 Java 中的请求/响应 DTO：它们在不可信 JSON 进入业务逻辑前
完成类型转换、必填检查、长度限制和额外字段拒绝。模型只描述数据，不执行数据库或 Agent
逻辑，避免协议层和业务层耦合。
"""

from __future__ import annotations

from datetime import datetime
from typing import Any, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class Actor(BaseModel):
    """Java 已认证的当前操作者及其本次动态工具权限。

    `allowedTools` 不是前端自行声明的权限，而是 Java 根据登录用户和当前角色计算后下发。
    Python 会据此构造最小工具集合，但 Java 在真正执行工具时仍会再次验签。
    """

    model_config = ConfigDict(extra="forbid")

    userId: int
    displayName: str | None = None
    activeRole: str
    roles: list[str]
    allowedTools: list[str]


class RunRequest(BaseModel):
    """Java 发给 Python 的一次完整 Agent 运行请求。"""

    model_config = ConfigDict(extra="forbid")

    runId: UUID
    conversationId: UUID
    message: str = Field(min_length=1, max_length=4000)
    actor: Actor
    toolToken: str = Field(min_length=1)


class ToolInvocation(BaseModel):
    """Python 回调 Java 白名单工具接口时发送的请求体。"""

    runId: UUID
    toolName: str
    arguments: dict[str, Any]


class JavaResult(BaseModel):
    """Java 统一 Result 包装的最小解析结构。"""

    code: int
    message: str
    data: Any | None = None


class KnowledgeUpsertRequest(BaseModel):
    """知识文档新增或按 `sourceUri` 更新的请求。"""

    model_config = ConfigDict(extra="forbid")

    actorId: int
    title: str = Field(min_length=1, max_length=200)
    sourceUri: str = Field(min_length=1, max_length=1000)
    sourceType: Literal["TEXT", "MARKDOWN", "POLICY", "LESSON", "TEXTBOOK"] = "TEXT"
    content: str = Field(min_length=20, max_length=100_000)
    metadata: dict[str, Any] = Field(default_factory=dict)


class KnowledgeDocumentView(BaseModel):
    """对外暴露的知识文档摘要，不返回原文和向量。"""

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
    """用户对某次 Agent 运行的正向或负向反馈。"""

    model_config = ConfigDict(extra="forbid")

    runId: UUID
    actorId: int
    rating: Literal[-1, 1]
    comment: str | None = Field(default=None, max_length=1000)


class ForgetMemoryRequest(BaseModel):
    """删除某个用户、某段会话相关长期记忆与 checkpoint 的请求。"""

    model_config = ConfigDict(extra="forbid")

    actorId: int
    conversationId: UUID
