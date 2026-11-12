from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


class ConversationMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=4000)


class AssistantRequest(BaseModel):
    store_id: int | None = Field(default=None, gt=0)
    branch_id: int | None = Field(default=None, gt=0)
    message: str = Field(min_length=2, max_length=2000)
    conversation: list[ConversationMessage] = Field(
        default_factory=list, max_length=10
    )
    assistant_scope: Literal["manager", "system"] = "manager"
    system_context: dict[str, Any] | None = None


class AssistantSource(BaseModel):
    type: Literal[
        "policy",
        "recommendation",
        "inventory",
        "product",
        "dashboard",
        "system",
    ]
    label: str
    document: str | None = None
    section: str | None = None


class AssistantResponse(BaseModel):
    answer: str
    tools_used: list[str]
    sources: list[AssistantSource]
    request_id: str


class AssistantHealthResponse(BaseModel):
    status: Literal["UP", "DOWN"]
    assistant_loaded: bool
    policy_index_loaded: bool
