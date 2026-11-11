import pandas as pd

from scripts.evaluate_recommender import (
    evaluate_models,
    temporal_train_test_split,
)


def create_evaluation_items() -> pd.DataFrame:
    rows = []

    transaction_id = 1

    for day in range(1, 21):
        products = (
            [10, 20]
            if day <= 15
            else [10, 20, 30]
        )

        for product_id in products:
            rows.append({
                "transaction_id":
                    transaction_id,
                "product_id":
                    product_id,
                "quantity":
                    1,
                "occurred_at":
                    f"2026-10-{day:02d} 10:00:00",
            })

        transaction_id += 1

    return pd.DataFrame(rows)


def test_temporal_split_has_no_overlap() -> None:
    items = create_evaluation_items()

    train_items, test_items = (
        temporal_train_test_split(
            items,
            train_ratio=0.80,
        )
    )

    train_ids = set(
        train_items["transaction_id"]
    )

    test_ids = set(
        test_items["transaction_id"]
    )

    assert train_ids.isdisjoint(test_ids)

    assert (
        train_items["occurred_at"].max()
        <= test_items["occurred_at"].min()
    )


def test_evaluation_compares_both_models() -> None:
    items = create_evaluation_items()

    results = evaluate_models(
        items,
        train_ratio=0.80,
        top_k=2,
        min_support=0.10,
        min_confidence=0.20,
        min_lift=0.0,
    )

    assert "popularity_baseline" in (
        results["models"]
    )

    assert "association_rule_hybrid" in (
        results["models"]
    )

    hybrid_metrics = results["models"][
        "association_rule_hybrid"
    ]

    assert (
        hybrid_metrics["evaluation_cases"]
        > 0
    )

    assert 0 <= (
        hybrid_metrics["precision_at_2"]
    ) <= 1

    assert 0 <= (
        hybrid_metrics["recall_at_2"]
    ) <= 1

    assert 0 <= (
        hybrid_metrics["catalog_coverage"]
    ) <= 1