from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator


class SpringApiModel(BaseModel):
    model_config = ConfigDict(extra="ignore")


class ProductSummary(SpringApiModel):
    id: int
    name: str
    sku: str | None = None
    sellingPrice: float | None = None
    brand: str | None = None
    storeId: int | None = None
    categoryId: int | None = None


class InventoryProduct(SpringApiModel):
    id: int
    name: str
    sku: str | None = None
    sellingPrice: float | None = None


class InventoryResponse(SpringApiModel):
    id: int | None = None
    branchId: int | None = None
    productId: int | None = None
    quantity: int
    product: InventoryProduct | None = None
    stocked: bool = True


class SearchProductsArguments(BaseModel):
    store_id: int = Field(gt=0)
    query: str = Field(min_length=2, max_length=100)
    limit: int = Field(default=5, ge=1, le=10)


class GetInventoryArguments(BaseModel):
    branch_id: int = Field(gt=0)
    product_id: int = Field(gt=0)


class GetRecommendationsArguments(BaseModel):
    store_id: int = Field(gt=0)
    product_ids: list[int] = Field(min_length=1, max_length=20)
    limit: int = Field(default=3, ge=1, le=5)


class GetDashboardSummaryArguments(BaseModel):
    branch_id: int = Field(gt=0)
    period: str = "7D"

    @field_validator("period", mode="before")
    @classmethod
    def normalize_period(cls, value: object) -> str:
        normalized = str(value or "7D").strip().upper().replace("_", " ")
        aliases = {
            "TODAY": "TODAY",
            "1D": "TODAY",
            "7D": "7D",
            "7 DAYS": "7D",
            "LAST 7 DAYS": "7D",
            "WEEK": "7D",
            "THIS WEEK": "7D",
            "30D": "30D",
            "30 DAYS": "30D",
            "LAST 30 DAYS": "30D",
            "MONTH": "30D",
            "3M": "3M",
            "3 MONTHS": "3M",
            "1Y": "1Y",
            "YEAR": "1Y",
        }
        return aliases.get(normalized, "7D")


class SearchPoliciesArguments(BaseModel):
    query: str = Field(min_length=2, max_length=500)
    limit: int = Field(default=3, ge=1, le=5)


class ToolDefinition(BaseModel):
    name: str
    description: str
    input_schema: dict[str, Any]


class ToolResult(BaseModel):
    tool_name: str
    data: dict[str, Any]
