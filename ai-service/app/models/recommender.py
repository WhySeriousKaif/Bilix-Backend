from collections.abc import Iterable

import pandas as pd
from mlxtend.frequent_patterns import (
    association_rules,
    fpgrowth,
)

from app.data.basket_preprocessor import (
    create_transaction_product_matrix,
    prepare_basket_items,
)


RULE_COLUMNS = [
    "antecedents",
    "consequents",
    "support",
    "confidence",
    "lift",
]


def train_popularity_model(
    items: pd.DataFrame,
) -> pd.DataFrame:
    """Rank products by the number of baskets containing them."""
    prepared_items = prepare_basket_items(items)
    total_baskets = prepared_items[
        "transaction_id"
    ].nunique()

    popularity = (
        prepared_items.groupby(
            "product_id",
            as_index=False,
        )
        .agg(
            basket_count=(
                "transaction_id",
                "nunique",
            )
        )
    )
    popularity["support"] = (
        popularity["basket_count"] / total_baskets
    )

    return popularity.sort_values(
        ["basket_count", "product_id"],
        ascending=[False, True],
    ).reset_index(drop=True)


def train_association_rules(
    items: pd.DataFrame,
    *,
    min_support: float = 0.05,
    min_confidence: float = 0.20,
    min_lift: float = 1.0,
) -> pd.DataFrame:
    """Train pairwise association rules with FP-Growth."""
    _validate_probability(
        "min_support",
        min_support,
    )
    _validate_probability(
        "min_confidence",
        min_confidence,
    )

    if min_lift < 0:
        raise ValueError(
            "min_lift must be greater than or equal to 0."
        )

    matrix = create_transaction_product_matrix(items)
    frequent_itemsets = fpgrowth(
        matrix,
        min_support=min_support,
        use_colnames=True,
        max_len=2,
    )

    if frequent_itemsets.empty or not (
        frequent_itemsets["itemsets"].str.len() > 1
    ).any():
        return pd.DataFrame(columns=RULE_COLUMNS)

    rules = association_rules(
        frequent_itemsets,
        num_itemsets=len(matrix),
        metric="confidence",
        min_threshold=min_confidence,
        return_metrics=[
            "support",
            "confidence",
            "lift",
        ],
    )
    rules = rules[rules["lift"] >= min_lift]

    return (
        rules[RULE_COLUMNS]
        .sort_values(
            ["lift", "confidence", "support"],
            ascending=False,
        )
        .reset_index(drop=True)
    )


def recommend_products(
    scanned_product_ids: Iterable[int],
    rules: pd.DataFrame,
    popularity: pd.DataFrame,
    *,
    top_k: int = 3,
) -> list[dict[str, int | float | str]]:
    """Recommend rule matches, then fill remaining slots by popularity."""
    if top_k <= 0:
        raise ValueError(
            "top_k must be greater than zero."
        )

    scanned = {
        int(product_id)
        for product_id in scanned_product_ids
    }
    candidates: dict[int, dict[str, int | float | str]] = {}

    for rule in rules.itertuples(index=False):
        antecedents = set(rule.antecedents)

        if not antecedents.issubset(scanned):
            continue

        for product_id in rule.consequents:
            product_id = int(product_id)

            if product_id in scanned:
                continue

            candidate = {
                "product_id": product_id,
                "score": float(
                    rule.confidence * rule.lift
                ),
                "confidence": float(rule.confidence),
                "lift": float(rule.lift),
                "reason": "association_rule",
            }
            existing = candidates.get(product_id)

            if (
                existing is None
                or candidate["score"] > existing["score"]
            ):
                candidates[product_id] = candidate

    recommendations = sorted(
        candidates.values(),
        key=lambda candidate: (
            -float(candidate["score"]),
            int(candidate["product_id"]),
        ),
    )

    selected_ids = scanned | {
        int(candidate["product_id"])
        for candidate in recommendations
    }

    for row in popularity.itertuples(index=False):
        if len(recommendations) >= top_k:
            break

        product_id = int(row.product_id)

        if product_id in selected_ids:
            continue

        recommendations.append({
            "product_id": product_id,
            "score": float(row.support),
            "confidence": 0.0,
            "lift": 0.0,
            "reason": "popularity_fallback",
        })
        selected_ids.add(product_id)

    return recommendations[:top_k]


def _validate_probability(
    name: str,
    value: float,
) -> None:
    if not 0 < value <= 1:
        raise ValueError(
            f"{name} must be greater than 0 and at most 1."
        )
