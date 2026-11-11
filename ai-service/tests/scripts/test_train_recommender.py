import json
from pathlib import Path

import joblib
import pandas as pd

from scripts.train_recommender import (
    train_and_save,
)


def test_training_pipeline_saves_artifacts(
    tmp_path: Path,
) -> None:
    items = pd.DataFrame(
        {
            "transaction_id": [
                1,
                1,
                2,
                2,
                3,
                3,
                4,
                4,
            ],
            "product_id": [
                10,
                20,
                10,
                20,
                10,
                20,
                10,
                30,
            ],
            "quantity": [
                1,
                1,
                1,
                1,
                1,
                1,
                1,
                1,
            ],
        }
    )

    input_path = tmp_path / "items.csv"
    output_directory = tmp_path / "models"

    items.to_csv(
        input_path,
        index=False,
    )

    metadata = train_and_save(
        input_path=input_path,
        output_directory=output_directory,
        min_support=0.25,
        min_confidence=0.50,
        min_lift=1.0,
    )

    popularity_path = (
        output_directory / "popularity.joblib"
    )

    rules_path = (
        output_directory
        / "association_rules.joblib"
    )

    metadata_path = (
        output_directory / "metadata.json"
    )

    assert popularity_path.exists()
    assert rules_path.exists()
    assert metadata_path.exists()

    popularity = joblib.load(popularity_path)
    rules = joblib.load(rules_path)

    assert not popularity.empty
    assert not rules.empty

    assert metadata["transaction_count"] == 4
    assert metadata["product_count"] == 3

    saved_metadata = json.loads(
        metadata_path.read_text(
            encoding="utf-8"
        )
    )

    assert saved_metadata["model_version"] == "1.0.0"