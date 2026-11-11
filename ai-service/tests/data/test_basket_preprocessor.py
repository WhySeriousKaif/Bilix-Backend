import pandas as pd
import pytest

from app.data.basket_preprocessor import (
    create_transaction_baskets,
    create_transaction_product_matrix,
    prepare_basket_items,
    validate_item_data,
)


@pytest.fixture
def sample_items() -> pd.DataFrame:
    return pd.DataFrame(
        {
            "transaction_id": [
                101,
                101,
                102,
                102,
                102,
            ],
            "product_id": [
                3,
                9,
                1,
                7,
                7,
            ],
            "quantity": [
                1,
                1,
                1,
                1,
                2,
            ],
        }
    )


def test_valid_item_data_does_not_raise(
    sample_items: pd.DataFrame,
) -> None:
    validate_item_data(sample_items)


def test_missing_required_column_raises_error() -> None:
    items = pd.DataFrame(
        {
            "transaction_id": [101],
            "quantity": [1],
        }
    )

    with pytest.raises(
        ValueError,
        match="Missing required columns: product_id",
    ):
        validate_item_data(items)


def test_non_positive_quantity_raises_error() -> None:
    items = pd.DataFrame(
        {
            "transaction_id": [101],
            "product_id": [3],
            "quantity": [0],
        }
    )

    with pytest.raises(
        ValueError,
        match="greater than zero",
    ):
        validate_item_data(items)


def test_duplicate_products_are_combined(
    sample_items: pd.DataFrame,
) -> None:
    prepared_items = prepare_basket_items(
        sample_items
    )

    product_row = prepared_items[
        (
            prepared_items["transaction_id"]
            == 102
        )
        & (
            prepared_items["product_id"]
            == 7
        )
    ]

    assert len(product_row) == 1
    assert product_row.iloc[0]["quantity"] == 3


def test_transaction_baskets_are_created(
    sample_items: pd.DataFrame,
) -> None:
    baskets = create_transaction_baskets(
        sample_items
    )

    assert baskets == {
        101: [3, 9],
        102: [1, 7],
    }


def test_boolean_matrix_is_created(
    sample_items: pd.DataFrame,
) -> None:
    matrix = create_transaction_product_matrix(
        sample_items
    )

    assert matrix.shape == (2, 4)

    assert bool(matrix.loc[101, 3]) is True
    assert bool(matrix.loc[101, 9]) is True
    assert bool(matrix.loc[101, 1]) is False

    assert bool(matrix.loc[102, 1]) is True
    assert bool(matrix.loc[102, 7]) is True
    assert bool(matrix.loc[102, 3]) is False