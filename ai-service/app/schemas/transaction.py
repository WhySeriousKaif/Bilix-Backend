from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field


def to_camel(value: str) -> str:
    first, *remaining = value.split("_")

    return first + "".join(
        word.capitalize()
        for word in remaining
    )


class ApiModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
    )


class TransactionItem(ApiModel):
    product_id: int
    product_sku: str
    product_name: str
    category_id: int | None = None
    category_name: str | None = None
    quantity: int = Field(gt=0)
    unit_price: Decimal = Field(ge=0)
    line_total: Decimal = Field(ge=0)


class Transaction(ApiModel):
    transaction_id: int
    store_id: int
    branch_id: int
    occurred_at: datetime
    payment_type: str | None = None
    total_amount: Decimal = Field(ge=0)
    items: list[TransactionItem] = Field(
        min_length=1
    )
    