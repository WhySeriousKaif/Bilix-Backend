import json
from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest

from app.schemas.assistant import AssistantRequest
from app.schemas.assistant_tools import ToolDefinition, ToolResult
from app.services.assistant_service import AssistantService


class FakeTools:
    def __init__(self) -> None:
        self.execute = AsyncMock()

    @staticmethod
    def definitions() -> list[ToolDefinition]:
        return [
            ToolDefinition(
                name="get_inventory",
                description="Get inventory.",
                input_schema={"type": "object", "properties": {}},
            ),
            ToolDefinition(
                name="search_policies",
                description="Search policies.",
                input_schema={"type": "object", "properties": {}},
            ),
        ]


class FakeResponses:
    def __init__(self, responses: list[object]) -> None:
        self._responses = responses
        self.calls: list[dict] = []

    async def create(self, **kwargs):
        self.calls.append(kwargs)
        return self._responses.pop(0)


def final_response(text: str):
    return SimpleNamespace(output=[], output_text=text)


def tool_response(name: str, arguments: dict, call_id: str = "call-1"):
    call = SimpleNamespace(
        type="function_call",
        name=name,
        arguments=json.dumps(arguments),
        call_id=call_id,
    )
    return SimpleNamespace(output=[call], output_text="")


@pytest.mark.asyncio
async def test_direct_answer_without_tool() -> None:
    tools = FakeTools()
    service = AssistantService(
        responses_client=FakeResponses([final_response("Hello manager.")]),
        tools=tools,
        model="test-model",
    )
    result = await service.answer(
        AssistantRequest(store_id=1, branch_id=2, message="Hello")
    )
    assert result.answer == "Hello manager."
    assert result.tools_used == []
    tools.execute.assert_not_awaited()


@pytest.mark.asyncio
async def test_tool_call_is_executed_and_returned_to_model() -> None:
    tools = FakeTools()
    tools.execute.return_value = ToolResult(
        tool_name="get_inventory",
        data={"product_id": 9, "quantity": 26, "available": True},
    )
    responses = FakeResponses(
        [
            tool_response("get_inventory", {"branch_id": 999, "product_id": 9}),
            final_response("Chocolate has 26 units available."),
        ]
    )
    service = AssistantService(
        responses_client=responses,
        tools=tools,
        model="test-model",
    )
    result = await service.answer(
        AssistantRequest(store_id=1, branch_id=2, message="Is chocolate available?")
    )
    tools.execute.assert_awaited_once_with(
        "get_inventory", {"branch_id": 2, "product_id": 9}
    )
    assert result.tools_used == ["get_inventory"]
    assert result.sources[0].type == "inventory"
    assert "26 units" in result.answer
    assert any(
        isinstance(item, dict) and item.get("type") == "function_call_output"
        for item in responses.calls[1]["input"]
    )


@pytest.mark.asyncio
async def test_policy_citations_are_exposed() -> None:
    tools = FakeTools()
    tools.execute.return_value = ToolResult(
        tool_name="search_policies",
        data={
            "query": "opened products",
            "citations": [
                {
                    "title": "Refund Policy",
                    "section": "Opened products",
                    "document": "refund-policy.md",
                    "excerpt": "Opened food is not normally returnable.",
                    "score": 0.82,
                    "chunk_id": "refund-1",
                }
            ],
        },
    )
    service = AssistantService(
        responses_client=FakeResponses(
            [
                tool_response("search_policies", {"query": "opened products"}),
                final_response("Opened food is not normally returnable."),
            ]
        ),
        tools=tools,
        model="test-model",
    )
    result = await service.answer(
        AssistantRequest(store_id=1, branch_id=2, message="Can opened food be returned?")
    )
    assert result.tools_used == ["search_policies"]
    assert result.sources[0].document == "refund-policy.md"
    assert result.sources[0].section == "Opened products"
