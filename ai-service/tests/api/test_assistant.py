from pathlib import Path
from unittest.mock import AsyncMock

from fastapi.testclient import TestClient

from app.main import create_app
from app.schemas.assistant import AssistantResponse, AssistantSource
from tests.api.test_recommendations import create_model_artifacts


class FakeAssistantService:
    def __init__(self) -> None:
        self.answer = AsyncMock(
            return_value=AssistantResponse(
                answer="Opened food is not normally returnable unless defective.",
                tools_used=["search_policies"],
                sources=[
                    AssistantSource(
                        type="policy",
                        label="Refund Policy — Opened products",
                        document="refund-policy.md",
                        section="Opened products",
                    )
                ],
                request_id="request-test-1",
            )
        )


def test_assistant_health_reports_loaded(
    tmp_path: Path,
) -> None:
    create_model_artifacts(tmp_path)
    application = create_app(
        model_directory=tmp_path,
        assistant_service=FakeAssistantService(),
    )
    with TestClient(application) as client:
        response = client.get("/assistant/health")
    assert response.status_code == 200
    assert response.json() == {
        "status": "UP",
        "assistant_loaded": True,
        "policy_index_loaded": True,
    }


def test_assistant_chat_returns_grounded_response(
    tmp_path: Path,
) -> None:
    create_model_artifacts(tmp_path)
    assistant = FakeAssistantService()
    application = create_app(
        model_directory=tmp_path,
        assistant_service=assistant,
    )
    payload = {
        "store_id": 1,
        "branch_id": 2,
        "message": "Can opened food be returned?",
        "conversation": [],
    }
    with TestClient(application) as client:
        response = client.post(
            "/assistant/chat",
            json=payload,
        )
    assert response.status_code == 200
    body = response.json()
    assert body["tools_used"] == ["search_policies"]
    assert body["sources"][0]["document"] == "refund-policy.md"
    assistant.answer.assert_awaited_once()
    request = assistant.answer.await_args.args[0]
    assert request.store_id == 1
    assert request.branch_id == 2


def test_assistant_chat_rejects_invalid_request(
    tmp_path: Path,
) -> None:
    create_model_artifacts(tmp_path)
    application = create_app(
        model_directory=tmp_path,
        assistant_service=FakeAssistantService(),
    )
    with TestClient(application) as client:
        response = client.post(
            "/assistant/chat",
            json={
                "store_id": 0,
                "branch_id": -1,
                "message": "x",
            },
        )
    assert response.status_code == 422


def test_assistant_returns_503_when_not_configured(
    tmp_path: Path,
    monkeypatch,
) -> None:
    create_model_artifacts(tmp_path)
    monkeypatch.setenv("OPENAI_API_KEY", "")
    from app.core.config import get_settings

    get_settings.cache_clear()
    application = create_app(model_directory=tmp_path)
    with TestClient(application) as client:
        health = client.get("/assistant/health")
        response = client.post(
            "/assistant/chat",
            json={
                "store_id": 1,
                "branch_id": 1,
                "message": "What is the refund policy?",
            },
        )
    assert health.json()["status"] == "DOWN"
    assert response.status_code == 503
    get_settings.cache_clear()
