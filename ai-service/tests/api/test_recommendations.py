import json
from pathlib import Path

import joblib
import pandas as pd
from fastapi.testclient import TestClient

from app.main import create_app


def create_model_artifacts(
    directory: Path,
) -> None:
    rules = pd.DataFrame(
        {
            "antecedents": [
                frozenset({3}),
            ],
            "consequents": [
                frozenset({9}),
            ],
            "support": [0.30],
            "confidence": [0.90],
            "lift": [2.50],
        }
    )

    popularity = pd.DataFrame(
        {
            "product_id": [1, 7, 9],
            "basket_count": [100, 80, 70],
            "support": [0.50, 0.40, 0.35],
        }
    )

    metadata = {
        "model_name":
            "billix-basket-recommender",
        "model_version": "test-1.0",
        "rule_count": 1,
    }

    joblib.dump(
        rules,
        directory
        / "association_rules.joblib",
    )

    joblib.dump(
        popularity,
        directory
        / "popularity.joblib",
    )

    (
        directory / "metadata.json"
    ).write_text(
        json.dumps(metadata),
        encoding="utf-8",
    )


def test_health_reports_loaded_model(
    tmp_path: Path,
) -> None:
    create_model_artifacts(tmp_path)

    application = create_app(
        model_directory=tmp_path
    )

    with TestClient(application) as client:
        response = client.get("/health")

    assert response.status_code == 200

    assert response.json() == {
        "status": "UP",
        "model_loaded": True,
    }


def test_association_recommendation(
    tmp_path: Path,
) -> None:
    create_model_artifacts(tmp_path)

    application = create_app(
        model_directory=tmp_path
    )

    with TestClient(application) as client:
        response = client.post(
            "/recommendations",
            json={
                "store_id": 1,
                "product_ids": [3],
                "top_k": 2,
            },
        )

    assert response.status_code == 200

    response_body = response.json()

    assert response_body[
        "model_version"
    ] == "test-1.0"

    first_recommendation = (
        response_body["recommendations"][0]
    )

    assert (
        first_recommendation["product_id"]
        == 9
    )

    assert (
        first_recommendation["reason"]
        == "association_rule"
    )


def test_empty_cart_uses_popularity(
    tmp_path: Path,
) -> None:
    create_model_artifacts(tmp_path)

    application = create_app(
        model_directory=tmp_path
    )

    with TestClient(application) as client:
        response = client.post(
            "/recommendations",
            json={
                "store_id": 1,
                "product_ids": [],
                "top_k": 2,
            },
        )

    assert response.status_code == 200

    recommendations = response.json()[
        "recommendations"
    ]

    assert len(recommendations) == 2

    assert all(
        recommendation["reason"]
        == "popularity_fallback"
        for recommendation in recommendations
    )