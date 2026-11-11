import argparse
import json
from collections.abc import Callable
from pathlib import Path
from typing import Any

import pandas as pd

from app.data.basket_preprocessor import (
    create_transaction_baskets,
)
from app.models.recommender import (
    recommend_products,
    train_association_rules,
    train_popularity_model,
)


DEFAULT_INPUT_PATH = Path(
    "data/processed/transaction_items.csv"
)

DEFAULT_OUTPUT_PATH = Path(
    "data/evaluation/recommender_metrics.json"
)

RULE_COLUMNS = [
    "antecedents",
    "consequents",
    "support",
    "confidence",
    "lift",
]


def temporal_train_test_split(
    items: pd.DataFrame,
    *,
    train_ratio: float = 0.80,
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """
    Place older transactions in training and newer
    transactions in evaluation.
    """
    if not 0 < train_ratio < 1:
        raise ValueError(
            "train_ratio must be between 0 and 1."
        )

    required_columns = {
        "transaction_id",
        "occurred_at",
    }

    missing_columns = (
        required_columns - set(items.columns)
    )

    if missing_columns:
        formatted_columns = ", ".join(
            sorted(missing_columns)
        )

        raise ValueError(
            "Missing split columns: "
            f"{formatted_columns}"
        )

    working_items = items.copy()

    working_items["occurred_at"] = pd.to_datetime(
        working_items["occurred_at"]
    )

    ordered_transactions = (
        working_items.groupby(
            "transaction_id",
            as_index=False,
        )
        .agg(
            occurred_at=(
                "occurred_at",
                "min",
            )
        )
        .sort_values(
            [
                "occurred_at",
                "transaction_id",
            ]
        )
    )

    split_index = int(
        len(ordered_transactions) * train_ratio
    )

    if split_index == 0 or split_index == len(
        ordered_transactions
    ):
        raise ValueError(
            "The dataset is too small for the "
            "requested train-test split."
        )

    train_transaction_ids = set(
        ordered_transactions.iloc[
            :split_index
        ]["transaction_id"]
    )

    test_transaction_ids = set(
        ordered_transactions.iloc[
            split_index:
        ]["transaction_id"]
    )

    train_items = working_items[
        working_items["transaction_id"].isin(
            train_transaction_ids
        )
    ].copy()

    test_items = working_items[
        working_items["transaction_id"].isin(
            test_transaction_ids
        )
    ].copy()

    return train_items, test_items


def extract_product_ids(
    recommendations: list[
        dict[str, int | float | str]
    ],
) -> list[int]:
    return [
        int(recommendation["product_id"])
        for recommendation in recommendations
    ]


def evaluate_recommender(
    test_items: pd.DataFrame,
    recommendation_function: Callable[
        [list[int]],
        list[dict[str, int | float | str]],
    ],
    *,
    catalog_size: int,
    top_k: int,
) -> dict[str, float | int]:
    """
    Evaluate recommendations using leave-one-out testing.

    Every product in a multi-product basket becomes the
    hidden target once.
    """
    if top_k <= 0:
        raise ValueError(
            "top_k must be greater than zero."
        )

    test_baskets = create_transaction_baskets(
        test_items
    )

    evaluation_cases = 0
    hits = 0

    recommended_products: set[int] = set()

    for basket in test_baskets.values():
        if len(basket) < 2:
            continue

        for hidden_product in basket:
            visible_products = [
                product_id
                for product_id in basket
                if product_id != hidden_product
            ]

            recommendations = (
                recommendation_function(
                    visible_products
                )
            )

            recommended_ids = extract_product_ids(
                recommendations[:top_k]
            )

            evaluation_cases += 1

            recommended_products.update(
                recommended_ids
            )

            if hidden_product in recommended_ids:
                hits += 1

    if evaluation_cases == 0:
        raise ValueError(
            "No multi-product evaluation baskets "
            "were available."
        )

    precision_at_k = (
        hits / (evaluation_cases * top_k)
    )

    recall_at_k = hits / evaluation_cases

    hit_rate_at_k = hits / evaluation_cases

    coverage = (
        len(recommended_products) / catalog_size
        if catalog_size > 0
        else 0.0
    )

    return {
        "evaluation_cases": evaluation_cases,
        "hits": hits,
        f"precision_at_{top_k}": precision_at_k,
        f"recall_at_{top_k}": recall_at_k,
        f"hit_rate_at_{top_k}": hit_rate_at_k,
        "catalog_coverage": coverage,
    }


def evaluate_models(
    items: pd.DataFrame,
    *,
    train_ratio: float = 0.80,
    top_k: int = 3,
    min_support: float = 0.05,
    min_confidence: float = 0.20,
    min_lift: float = 1.0,
) -> dict[str, Any]:
    train_items, test_items = (
        temporal_train_test_split(
            items,
            train_ratio=train_ratio,
        )
    )

    popularity = train_popularity_model(
        train_items
    )

    rules = train_association_rules(
        train_items,
        min_support=min_support,
        min_confidence=min_confidence,
        min_lift=min_lift,
    )

    empty_rules = pd.DataFrame(
        columns=RULE_COLUMNS
    )

    catalog_size = items[
        "product_id"
    ].nunique()

    def popularity_recommender(
        visible_products: list[int],
    ) -> list[dict[str, int | float | str]]:
        return recommend_products(
            scanned_product_ids=visible_products,
            rules=empty_rules,
            popularity=popularity,
            top_k=top_k,
        )

    def hybrid_recommender(
        visible_products: list[int],
    ) -> list[dict[str, int | float | str]]:
        return recommend_products(
            scanned_product_ids=visible_products,
            rules=rules,
            popularity=popularity,
            top_k=top_k,
        )

    popularity_metrics = evaluate_recommender(
        test_items=test_items,
        recommendation_function=(
            popularity_recommender
        ),
        catalog_size=catalog_size,
        top_k=top_k,
    )

    hybrid_metrics = evaluate_recommender(
        test_items=test_items,
        recommendation_function=(
            hybrid_recommender
        ),
        catalog_size=catalog_size,
        top_k=top_k,
    )

    return {
        "evaluation_method": (
            "temporal split with leave-one-out "
            "basket evaluation"
        ),
        "train_ratio": train_ratio,
        "top_k": top_k,
        "train_transaction_count": int(
            train_items[
                "transaction_id"
            ].nunique()
        ),
        "test_transaction_count": int(
            test_items[
                "transaction_id"
            ].nunique()
        ),
        "trained_rule_count": int(
            len(rules)
        ),
        "parameters": {
            "min_support": min_support,
            "min_confidence": min_confidence,
            "min_lift": min_lift,
        },
        "models": {
            "popularity_baseline": (
                popularity_metrics
            ),
            "association_rule_hybrid": (
                hybrid_metrics
            ),
        },
    }


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Evaluate the Billix recommendation "
            "models using a temporal holdout."
        )
    )

    parser.add_argument(
        "--input",
        type=Path,
        default=DEFAULT_INPUT_PATH,
    )

    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT_PATH,
    )

    parser.add_argument(
        "--train-ratio",
        type=float,
        default=0.80,
    )

    parser.add_argument(
        "--top-k",
        type=int,
        default=3,
    )

    parser.add_argument(
        "--min-support",
        type=float,
        default=0.05,
    )

    parser.add_argument(
        "--min-confidence",
        type=float,
        default=0.20,
    )

    parser.add_argument(
        "--min-lift",
        type=float,
        default=1.0,
    )

    return parser.parse_args()


def main() -> None:
    arguments = parse_arguments()

    if not arguments.input.exists():
        raise FileNotFoundError(
            "Evaluation data not found: "
            f"{arguments.input}"
        )

    items = pd.read_csv(
        arguments.input
    )

    results = evaluate_models(
        items,
        train_ratio=arguments.train_ratio,
        top_k=arguments.top_k,
        min_support=arguments.min_support,
        min_confidence=(
            arguments.min_confidence
        ),
        min_lift=arguments.min_lift,
    )

    arguments.output.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    arguments.output.write_text(
        json.dumps(
            results,
            indent=2,
        ),
        encoding="utf-8",
    )

    print(
        json.dumps(
            results,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()