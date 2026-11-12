import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
import pytest
import respx
from httpx import Response

from app.clients.billix_client import BillixClient
from app.core.config import Settings
from app.services.assistant_tools import AssistantTools, UnknownToolError
from app.services.recommendation_service import RecommendationService
from app.rag.policy_retriever import PolicyRetriever
from app.rag.embedding_provider import normalize_rows


class FakeEmbeddingProvider:
    model_name = "fake-embedding-test"
    dimensions = 3

    def embed_documents(self, texts: list[str]) -> np.ndarray:
        return normalize_rows(
            np.asarray([[1.0, 0.0, 0.1] for _ in texts])
        )

    def embed_query(self, text: str) -> np.ndarray:
        return normalize_rows(np.asarray([1.0, 0.0, 0.1]))[0]


def create_recommendation_service(directory: Path) -> RecommendationService:
    rules = pd.DataFrame(
        {
            "antecedents": [frozenset({3})],
            "consequents": [frozenset({9})],
            "support": [0.30],
            "confidence": [0.90],
            "lift": [2.50],
        }
    )
    popularity = pd.DataFrame(
        {
            "product_id": [1, 7, 9],
            "basket_count": [100, 80, 70],
            "support": [0.50, 0.40, 0.35],
        }
    )
    joblib.dump(rules, directory / "association_rules.joblib")
    joblib.dump(popularity, directory / "popularity.joblib")
    (directory / "metadata.json").write_text(
        json.dumps({"model_version": "test-1.0"}),
        encoding="utf-8",
    )
    return RecommendationService.from_directory(directory)


@pytest.fixture
def tools(tmp_path: Path) -> AssistantTools:
    settings = Settings(
        spring_base_url="http://billix.test",
        spring_api_token="test-token",
        request_timeout_seconds=5,
        _env_file=None,
    )
    policy_directory = tmp_path / "policies"
    policy_directory.mkdir()
    (policy_directory / "refund-policy.md").write_text(
        "# Refund Policy\n\n## Opened products\n\n"
        "Opened food cannot be returned unless defective.",
        encoding="utf-8",
    )
    return AssistantTools(
        billix_client=BillixClient(settings),
        recommendation_service=create_recommendation_service(tmp_path),
        policy_retriever=PolicyRetriever.build(
            policy_directory,
            embedding_provider=FakeEmbeddingProvider(),
        ),
    )


def test_tool_definitions_are_available() -> None:
    definitions = AssistantTools.definitions()
    assert {definition.name for definition in definitions} == {
        "search_products",
        "get_inventory",
        "get_recommendations",
        "get_dashboard_summary",
        "search_policies",
    }
    assert all(definition.input_schema for definition in definitions)


@pytest.mark.asyncio
@respx.mock
async def test_search_products_tool(tools: AssistantTools) -> None:
    route = respx.get(
        "http://billix.test/api/products/search/1/coffee"
    ).mock(
        return_value=Response(
            200,
            json=[
                {
                    "id": 3,
                    "name": "Instant Coffee",
                    "sku": "COFFEE-001",
                    "sellingPrice": 220.0,
                    "brand": "WakeUp",
                    "storeId": 1,
                }
            ],
        )
    )
    result = await tools.execute(
        "search_products",
        {"store_id": 1, "query": "coffee", "limit": 5},
    )
    assert route.called
    assert result.tool_name == "search_products"
    assert result.data["products"][0]["product_id"] == 3
    assert result.data["products"][0]["name"] == "Instant Coffee"


@pytest.mark.asyncio
@respx.mock
async def test_inventory_tool(tools: AssistantTools) -> None:
    route = respx.get(
        "http://billix.test/api/inventories/product/9/branch/1"
    ).mock(
        return_value=Response(
            200,
            json={
                "id": 100,
                "branchId": 1,
                "productId": 9,
                "quantity": 26,
                "product": {
                    "id": 9,
                    "name": "Chocolate",
                    "sku": "CHOC-001",
                    "sellingPrice": 80.0,
                },
            },
        )
    )
    result = await tools.execute(
        "get_inventory",
        {"branch_id": 1, "product_id": 9},
    )
    assert route.called
    assert result.data["quantity"] == 26
    assert result.data["available"] is True
    assert result.data["product_name"] == "Chocolate"


@pytest.mark.asyncio
async def test_recommendation_tool(tools: AssistantTools) -> None:
    result = await tools.execute(
        "get_recommendations",
        {"store_id": 1, "product_ids": [3], "limit": 3},
    )
    recommendations = result.data["recommendations"]
    assert result.tool_name == "get_recommendations"
    assert recommendations[0]["product_id"] == 9
    assert recommendations[0]["confidence"] == 0.90
    assert result.data["model_version"] == "test-1.0"


@pytest.mark.asyncio
@respx.mock
async def test_dashboard_summary_tool(tools: AssistantTools) -> None:
    route = respx.get(
        "http://billix.test/api/dashboard/branch/1",
        params={"period": "7D"},
    ).mock(
        return_value=Response(
            200,
            json={
                "branchId": 1,
                "period": "7D",
                "metrics": {"totalSales": 743431.0, "totalOrders": 86},
            },
        )
    )
    result = await tools.execute(
        "get_dashboard_summary",
        {"branch_id": 1, "period": "7D"},
    )
    assert route.called
    assert result.tool_name == "get_dashboard_summary"
    assert result.data["metrics"]["totalOrders"] == 86


@pytest.mark.asyncio
async def test_unknown_tool_is_rejected(tools: AssistantTools) -> None:
    with pytest.raises(UnknownToolError, match="not allowed"):
        await tools.execute("delete_inventory", {"inventory_id": 10})


@pytest.mark.asyncio
async def test_invalid_arguments_are_rejected(tools: AssistantTools) -> None:
    with pytest.raises(ValueError):
        await tools.execute(
            "get_inventory",
            {"branch_id": -1, "product_id": 0},
        )


@pytest.mark.asyncio
async def test_policy_search_tool_returns_citations(
    tools: AssistantTools,
) -> None:
    result = await tools.execute(
        "search_policies",
        {"query": "Can opened food be returned?", "limit": 2},
    )
    assert result.tool_name == "search_policies"
    assert result.data["citations"]
    assert result.data["citations"][0]["document"] == "refund-policy.md"
