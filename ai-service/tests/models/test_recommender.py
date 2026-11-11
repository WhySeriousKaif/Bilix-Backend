import pandas as pd
import pytest

from app.models.recommender import (
    recommend_products,
    train_association_rules,
    train_popularity_model,
)


@pytest.fixture
def sample_items() -> pd.DataFrame:
    baskets = {
        1: [1, 2],
        2: [1, 2],
        3: [1, 2],
        4: [1, 3],
        5: [3, 4],
        6: [3, 4],
    }

    return pd.DataFrame([
        {
            "transaction_id": transaction_id,
            "product_id": product_id,
            "quantity": 1,
        }
        for transaction_id, product_ids in baskets.items()
        for product_id in product_ids
    ])


def test_popularity_model_ranks_products(
    sample_items: pd.DataFrame,
) -> None:
    popularity = train_popularity_model(sample_items)

    assert popularity.iloc[0]["product_id"] == 1
    assert popularity.iloc[0]["basket_count"] == 4
    assert popularity.iloc[0]["support"] == pytest.approx(
        4 / 6
    )


def test_association_rules_find_strong_pair(
    sample_items: pd.DataFrame,
) -> None:
    rules = train_association_rules(
        sample_items,
        min_support=0.30,
        min_confidence=0.50,
        min_lift=1.0,
    )

    matching_rule = rules[
        (rules["antecedents"] == frozenset({2}))
        & (rules["consequents"] == frozenset({1}))
    ]

    assert len(matching_rule) == 1
    assert matching_rule.iloc[0][
        "confidence"
    ] == pytest.approx(1.0)
    assert matching_rule.iloc[0][
        "lift"
    ] == pytest.approx(1.5)


def test_recommendation_uses_association_rule(
    sample_items: pd.DataFrame,
) -> None:
    popularity = train_popularity_model(sample_items)
    rules = train_association_rules(
        sample_items,
        min_support=0.30,
        min_confidence=0.50,
        min_lift=1.0,
    )

    recommendations = recommend_products(
        [2],
        rules,
        popularity,
        top_k=2,
    )

    assert recommendations[0]["product_id"] == 1
    assert recommendations[0]["reason"] == (
        "association_rule"
    )


def test_popularity_fills_missing_recommendations(
    sample_items: pd.DataFrame,
) -> None:
    popularity = train_popularity_model(sample_items)
    empty_rules = pd.DataFrame(
        columns=[
            "antecedents",
            "consequents",
            "support",
            "confidence",
            "lift",
        ]
    )

    recommendations = recommend_products(
        [],
        empty_rules,
        popularity,
        top_k=2,
    )

    assert len(recommendations) == 2
    assert all(
        result["reason"] == "popularity_fallback"
        for result in recommendations
    )


def test_scanned_product_is_not_recommended(
    sample_items: pd.DataFrame,
) -> None:
    popularity = train_popularity_model(sample_items)
    rules = train_association_rules(
        sample_items,
        min_support=0.30,
        min_confidence=0.50,
        min_lift=1.0,
    )

    recommendations = recommend_products(
        [1],
        rules,
        popularity,
        top_k=3,
    )

    assert 1 not in {
        result["product_id"]
        for result in recommendations
    }
