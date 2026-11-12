from __future__ import annotations

from typing import Any

from app.clients.billix_client import BillixClient
from app.schemas.assistant_tools import (
    GetInventoryArguments,
    GetDashboardSummaryArguments,
    GetRecommendationsArguments,
    SearchProductsArguments,
    SearchPoliciesArguments,
    ToolDefinition,
    ToolResult,
)
from app.rag.policy_retriever import PolicyRetriever
from app.services.recommendation_service import RecommendationService


class UnknownToolError(ValueError):
    pass


class ToolUnavailableError(RuntimeError):
    pass


class AssistantTools:
    def __init__(
        self,
        *,
        billix_client: BillixClient,
        recommendation_service: RecommendationService,
        policy_retriever: PolicyRetriever | None = None,
    ) -> None:
        self._billix_client = billix_client
        self._recommendation_service = recommendation_service
        self._policy_retriever = policy_retriever

    @staticmethod
    def definitions() -> list[ToolDefinition]:
        return [
            ToolDefinition(
                name="search_products",
                description=(
                    "Search products in a Billix store by product name, "
                    "SKU, or keyword. Use this first when the user gives "
                    "a product name instead of a product ID."
                ),
                input_schema=SearchProductsArguments.model_json_schema(),
            ),
            ToolDefinition(
                name="get_inventory",
                description=(
                    "Get the current inventory quantity for one product "
                    "in one branch."
                ),
                input_schema=GetInventoryArguments.model_json_schema(),
            ),
            ToolDefinition(
                name="get_recommendations",
                description=(
                    "Get cross-sell recommendations for products currently "
                    "in a customer's cart."
                ),
                input_schema=GetRecommendationsArguments.model_json_schema(),
            ),
            ToolDefinition(
                name="get_dashboard_summary",
                description=(
                    "Get the authenticated branch dashboard summary for a time "
                    "period, including sales, orders, customers, average order "
                    "value, payment breakdown, refunds, recent sales, top products, "
                    "order status, trends, and active staff."
                ),
                input_schema=GetDashboardSummaryArguments.model_json_schema(),
            ),
            ToolDefinition(
                name="search_policies",
                description=(
                    "Search Billix policy documents for refund, discount, "
                    "cashier procedure, and manager approval information."
                ),
                input_schema=SearchPoliciesArguments.model_json_schema(),
            ),
        ]

    async def execute(
        self,
        tool_name: str,
        arguments: dict[str, Any],
    ) -> ToolResult:
        if tool_name == "search_products":
            return await self._search_products(
                SearchProductsArguments.model_validate(arguments)
            )
        if tool_name == "get_inventory":
            return await self._get_inventory(
                GetInventoryArguments.model_validate(arguments)
            )
        if tool_name == "get_recommendations":
            return self._get_recommendations(
                GetRecommendationsArguments.model_validate(arguments)
            )
        if tool_name == "get_dashboard_summary":
            return await self._get_dashboard_summary(
                GetDashboardSummaryArguments.model_validate(arguments)
            )
        if tool_name == "search_policies":
            return self._search_policies(
                SearchPoliciesArguments.model_validate(arguments)
            )
        raise UnknownToolError(f"Tool is not allowed: {tool_name}")

    async def _get_dashboard_summary(
        self,
        arguments: GetDashboardSummaryArguments,
    ) -> ToolResult:
        dashboard = await self._billix_client.get_dashboard_summary(
            branch_id=arguments.branch_id,
            period=arguments.period,
        )
        return ToolResult(
            tool_name="get_dashboard_summary",
            data=dashboard,
        )

    def _search_policies(
        self,
        arguments: SearchPoliciesArguments,
    ) -> ToolResult:
        if self._policy_retriever is None:
            raise ToolUnavailableError("Policy search index is not loaded.")
        result = self._policy_retriever.search(
            arguments.query,
            top_k=arguments.limit,
        )
        return ToolResult(
            tool_name="search_policies",
            data=result.model_dump(),
        )

    async def _search_products(
        self,
        arguments: SearchProductsArguments,
    ) -> ToolResult:
        products = await self._billix_client.search_products(
            store_id=arguments.store_id,
            query=arguments.query,
            limit=arguments.limit,
        )
        return ToolResult(
            tool_name="search_products",
            data={
                "query": arguments.query,
                "store_id": arguments.store_id,
                "products": [
                    {
                        "product_id": product.id,
                        "name": product.name,
                        "sku": product.sku,
                        "selling_price": product.sellingPrice,
                        "brand": product.brand,
                    }
                    for product in products
                ],
            },
        )

    async def _get_inventory(
        self,
        arguments: GetInventoryArguments,
    ) -> ToolResult:
        inventory = await self._billix_client.get_inventory(
            branch_id=arguments.branch_id,
            product_id=arguments.product_id,
        )
        product_name = (
            inventory.product.name
            if inventory.product is not None
            else None
        )
        return ToolResult(
            tool_name="get_inventory",
            data={
                "branch_id": arguments.branch_id,
                "product_id": arguments.product_id,
                "product_name": product_name,
                "quantity": inventory.quantity,
                "available": inventory.quantity > 0,
            },
        )

    def _get_recommendations(
        self,
        arguments: GetRecommendationsArguments,
    ) -> ToolResult:
        recommendations = self._recommendation_service.recommend(
            product_ids=arguments.product_ids,
            top_k=arguments.limit,
        )
        return ToolResult(
            tool_name="get_recommendations",
            data={
                "store_id": arguments.store_id,
                "cart_product_ids": arguments.product_ids,
                "recommendations": recommendations,
                "model_version": self._recommendation_service.model_version,
            },
        )
