import pandas as pd

from app.schemas.transaction import Transaction


TRANSACTION_COLUMNS = [
    "transaction_id",
    "store_id",
    "branch_id",
    "occurred_at",
    "payment_type",
    "total_amount",
]

ITEM_COLUMNS = [
    "transaction_id",
    "store_id",
    "branch_id",
    "occurred_at",
    "product_id",
    "product_sku",
    "product_name",
    "category_id",
    "category_name",
    "quantity",
    "unit_price",
    "line_total",
]


def normalize_transactions(
    transactions: list[Transaction],
) -> tuple[pd.DataFrame, pd.DataFrame]:

    transaction_rows: list[dict] = []
    item_rows: list[dict] = []

    for transaction in transactions:
        transaction_rows.append({
            "transaction_id":
                transaction.transaction_id,
            "store_id":
                transaction.store_id,
            "branch_id":
                transaction.branch_id,
            "occurred_at":
                transaction.occurred_at,
            "payment_type":
                transaction.payment_type,
            "total_amount":
                float(transaction.total_amount),
        })

        for item in transaction.items:
            item_rows.append({
                "transaction_id":
                    transaction.transaction_id,
                "store_id":
                    transaction.store_id,
                "branch_id":
                    transaction.branch_id,
                "occurred_at":
                    transaction.occurred_at,
                "product_id":
                    item.product_id,
                "product_sku":
                    item.product_sku,
                "product_name":
                    item.product_name,
                "category_id":
                    item.category_id,
                "category_name":
                    item.category_name,
                "quantity":
                    item.quantity,
                "unit_price":
                    float(item.unit_price),
                "line_total":
                    float(item.line_total),
            })

    transactions_df = pd.DataFrame(
        transaction_rows,
        columns=TRANSACTION_COLUMNS,
    )

    items_df = pd.DataFrame(
        item_rows,
        columns=ITEM_COLUMNS,
    )

    if not transactions_df.empty:
        transactions_df["occurred_at"] = (
            pd.to_datetime(
                transactions_df["occurred_at"]
            )
        )

    if not items_df.empty:
        items_df["occurred_at"] = pd.to_datetime(
            items_df["occurred_at"]
        )

    return transactions_df, items_df