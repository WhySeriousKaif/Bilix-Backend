import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from app.models.recommender import (
    train_association_rules,
    train_popularity_model,
)


DEFAULT_INPUT_PATH = Path(
    "data/processed/transaction_items.csv"
)

DEFAULT_OUTPUT_DIRECTORY = Path("data/models")


def train_and_save(
    input_path: Path,
    output_directory: Path,
    *,
    min_support: float = 0.05,
    min_confidence: float = 0.20,
    min_lift: float = 1.0,
) -> dict[str, Any]:
    """
    Train the Billix recommendation models and save their artifacts.
    """
    if not input_path.exists():
        raise FileNotFoundError(
            f"Training data not found: {input_path}"
        )

    items = pd.read_csv(input_path)

    popularity = train_popularity_model(items)

    rules = train_association_rules(
        items,
        min_support=min_support,
        min_confidence=min_confidence,
        min_lift=min_lift,
    )

    output_directory.mkdir(
        parents=True,
        exist_ok=True,
    )

    popularity_path = (
        output_directory / "popularity.joblib"
    )

    rules_path = (
        output_directory / "association_rules.joblib"
    )

    metadata_path = (
        output_directory / "metadata.json"
    )

    joblib.dump(
        popularity,
        popularity_path,
    )

    joblib.dump(
        rules,
        rules_path,
    )

    metadata = {
        "model_name": "billix-basket-recommender",
        "model_version": "1.0.0",
        "algorithm": "FP-Growth association rules",
        "trained_at": datetime.now(
            timezone.utc
        ).isoformat(),
        "training_data": str(input_path),
        "transaction_count": int(
            items["transaction_id"].nunique()
        ),
        "item_row_count": int(len(items)),
        "product_count": int(
            items["product_id"].nunique()
        ),
        "rule_count": int(len(rules)),
        "parameters": {
            "min_support": min_support,
            "min_confidence": min_confidence,
            "min_lift": min_lift,
        },
        "artifacts": {
            "popularity": popularity_path.name,
            "association_rules": rules_path.name,
        },
    }

    with metadata_path.open(
        "w",
        encoding="utf-8",
    ) as metadata_file:
        json.dump(
            metadata,
            metadata_file,
            indent=2,
        )

    return metadata


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Train and save the Billix "
            "basket recommendation models."
        )
    )

    parser.add_argument(
        "--input",
        type=Path,
        default=DEFAULT_INPUT_PATH,
        help="Path to transaction_items.csv",
    )

    parser.add_argument(
        "--output-directory",
        type=Path,
        default=DEFAULT_OUTPUT_DIRECTORY,
        help="Directory where model artifacts are saved",
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

    metadata = train_and_save(
        input_path=arguments.input,
        output_directory=(
            arguments.output_directory
        ),
        min_support=arguments.min_support,
        min_confidence=arguments.min_confidence,
        min_lift=arguments.min_lift,
    )

    print("Recommendation training completed.")
    print(
        "Transactions:",
        metadata["transaction_count"],
    )
    print(
        "Products:",
        metadata["product_count"],
    )
    print(
        "Association rules:",
        metadata["rule_count"],
    )
    print(
        "Model version:",
        metadata["model_version"],
    )


if __name__ == "__main__":
    main()