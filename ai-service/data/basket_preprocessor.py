import pandas as pd


REQUIRED_ITEM_COLUMNS = {
    "transaction_id",
    "product_id",
    "quantity",
}


def validate_item_data(items: pd.DataFrame) -> None:
    """
    Validate item-level transaction data before basket creation.

    Raises ValueError when the data cannot safely be used for
    recommendation training.
    """
    if items.empty:
        raise ValueError(
            "Transaction item data cannot be empty."
        )

    missing_columns = (
        REQUIRED_ITEM_COLUMNS - set(items.columns)
    )

    if missing_columns:
        formatted_columns = ", ".join(
            sorted(missing_columns)
        )

        raise ValueError(
            "Missing required columns: "
            f"{formatted_columns}"
        )

    required_data = items[
        [
            "transaction_id",
            "product_id",
            "quantity",
        ]
    ]

    if required_data.isna().any().any():
        raise ValueError(
            "Required basket columns cannot "
            "contain missing values."
        )

    if not pd.api.types.is_numeric_dtype(
        items["transaction_id"]
    ):
        raise ValueError(
            "transaction_id must be numeric."
        )

    if not pd.api.types.is_numeric_dtype(
        items["product_id"]
    ):
        raise ValueError(
            "product_id must be numeric."
        )

    if not pd.api.types.is_numeric_dtype(
        items["quantity"]
    ):
        raise ValueError(
            "quantity must be numeric."
        )

    if (items["quantity"] <= 0).any():
        raise ValueError(
            "All product quantities must "
            "be greater than zero."
        )


def prepare_basket_items(
    items: pd.DataFrame,
) -> pd.DataFrame:
    """
    Validate and aggregate item rows.

    If the same product appears more than once in one
    transaction, its quantities are combined.
    """
    validate_item_data(items)

    prepared_items = (
        items[
            [
                "transaction_id",
                "product_id",
                "quantity",
            ]
        ]
        .groupby(
            [
                "transaction_id",
                "product_id",
            ],
            as_index=False,
        )
        .agg(quantity=("quantity", "sum"))
        .sort_values(
            [
                "transaction_id",
                "product_id",
            ]
        )
        .reset_index(drop=True)
    )

    return prepared_items


def create_transaction_baskets(
    items: pd.DataFrame,
) -> dict[int, list[int]]:
    """
    Create one list of unique product IDs per transaction.

    Example:
        {
            101: [3, 9],
            102: [1, 7],
        }
    """
    prepared_items = prepare_basket_items(items)

    grouped_baskets = (
        prepared_items.groupby("transaction_id")[
            "product_id"
        ]
        .apply(lambda values: sorted(values.tolist()))
    )

    return {
        int(transaction_id): [
            int(product_id)
            for product_id in product_ids
        ]
        for transaction_id, product_ids
        in grouped_baskets.items()
    }


def create_transaction_product_matrix(
    items: pd.DataFrame,
) -> pd.DataFrame:
    """
    Create a Boolean transaction-product matrix.

    Rows represent transactions.
    Columns represent products.
    True means the product exists in the transaction.
    """
    prepared_items = prepare_basket_items(items)

    matrix = pd.crosstab(
        prepared_items["transaction_id"],
        prepared_items["product_id"],
    ).gt(0)

    matrix.index.name = "transaction_id"
    matrix.columns.name = "product_id"

    return matrix.sort_index().sort_index(axis=1)