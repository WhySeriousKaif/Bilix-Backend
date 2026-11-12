from pathlib import Path

import numpy as np
import pytest

from app.rag.embedding_provider import normalize_rows
from app.rag.policy_retriever import PolicyRetriever, chunk_markdown


class FakeEmbeddingProvider:
    model_name = "fake-embedding-test"
    dimensions = 4

    @staticmethod
    def _vector(text: str) -> list[float]:
        lowered = text.lower()
        return [
            float(sum(word in lowered for word in ("refund", "return", "opened", "payment", "food"))),
            float(sum(word in lowered for word in ("discount", "percent", "approval", "approves"))),
            float("cashier" in lowered),
            0.1,
        ]

    def embed_documents(self, texts: list[str]) -> np.ndarray:
        return normalize_rows(np.asarray([self._vector(text) for text in texts]))

    def embed_query(self, text: str) -> np.ndarray:
        return normalize_rows(np.asarray(self._vector(text)))[0]


@pytest.fixture
def policy_directory(tmp_path: Path) -> Path:
    directory = tmp_path / "policies"
    directory.mkdir()
    (directory / "refund-policy.md").write_text(
        """# Refund Policy

## Opened products

Opened food cannot normally be returned unless it is defective.

## Refund method

Approved refunds use the original payment method.
""",
        encoding="utf-8",
    )
    (directory / "discount-policy.md").write_text(
        """# Discount Policy

## Manager approval

Discounts above five percent require manager approval.
""",
        encoding="utf-8",
    )
    return directory


def test_markdown_is_chunked_by_section(policy_directory: Path) -> None:
    chunks = chunk_markdown(policy_directory / "refund-policy.md")
    assert len(chunks) == 2
    assert chunks[0].title == "Refund Policy"
    assert chunks[0].section == "Opened products"


def test_refund_question_returns_refund_policy(policy_directory: Path) -> None:
    retriever = PolicyRetriever.build(
        policy_directory, embedding_provider=FakeEmbeddingProvider()
    )
    result = retriever.search("Can opened food be returned?", top_k=2)
    assert result.citations
    assert result.citations[0].document == "refund-policy.md"
    assert result.citations[0].section == "Opened products"


def test_discount_question_returns_discount_policy(policy_directory: Path) -> None:
    retriever = PolicyRetriever.build(
        policy_directory, embedding_provider=FakeEmbeddingProvider()
    )
    result = retriever.search("Who approves a ten percent discount?")
    assert result.citations[0].document == "discount-policy.md"
    assert result.citations[0].section == "Manager approval"


def test_index_can_be_saved_and_loaded(
    policy_directory: Path,
    tmp_path: Path,
) -> None:
    index_path = tmp_path / "policy_index.joblib"
    provider = FakeEmbeddingProvider()
    original = PolicyRetriever.build(
        policy_directory, embedding_provider=provider
    )
    original.save(index_path)
    loaded = PolicyRetriever.load(index_path, embedding_provider=provider)
    result = loaded.search("original payment method")
    assert loaded.chunk_count == original.chunk_count
    assert result.citations[0].section == "Refund method"


def test_blank_query_is_rejected(policy_directory: Path) -> None:
    retriever = PolicyRetriever.build(
        policy_directory, embedding_provider=FakeEmbeddingProvider()
    )
    with pytest.raises(ValueError, match="at least two"):
        retriever.search(" ")
