import json
from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from app.models.recommender import (
    recommend_products,
)


class RecommendationService:
    def __init__(
        self,
        rules: pd.DataFrame,
        popularity: pd.DataFrame,
        metadata: dict[str, Any],
    ) -> None:
        self._rules = rules
        self._popularity = popularity
        self._metadata = metadata

    @classmethod
    def from_directory(
        cls,
        model_directory: Path,
    ) -> "RecommendationService":
        rules_path = (
            model_directory
            / "association_rules.joblib"
        )

        popularity_path = (
            model_directory
            / "popularity.joblib"
        )

        metadata_path = (
            model_directory
            / "metadata.json"
        )

        required_paths = [
            rules_path,
            popularity_path,
            metadata_path,
        ]

        missing_paths = [
            path
            for path in required_paths
            if not path.exists()
        ]

        if missing_paths:
            formatted_paths = ", ".join(
                str(path)
                for path in missing_paths
            )

            raise FileNotFoundError(
                "Missing model artifacts: "
                f"{formatted_paths}"
            )

        rules = joblib.load(rules_path)
        popularity = joblib.load(
            popularity_path
        )

        metadata = json.loads(
            metadata_path.read_text(
                encoding="utf-8"
            )
        )

        if not isinstance(
            rules,
            pd.DataFrame,
        ):
            raise TypeError(
                "Association-rule artifact "
                "must be a DataFrame."
            )

        if not isinstance(
            popularity,
            pd.DataFrame,
        ):
            raise TypeError(
                "Popularity artifact must "
                "be a DataFrame."
            )

        return cls(
            rules=rules,
            popularity=popularity,
            metadata=metadata,
        )

    @property
    def model_version(self) -> str:
        return str(
            self._metadata.get(
                "model_version",
                "unknown",
            )
        )

    @property
    def metadata(self) -> dict[str, Any]:
        return self._metadata.copy()

    def recommend(
        self,
        product_ids: list[int],
        *,
        top_k: int,
    ) -> list[dict[str, int | float | str]]:
        return recommend_products(
            scanned_product_ids=product_ids,
            rules=self._rules,
            popularity=self._popularity,
            top_k=top_k,
        )