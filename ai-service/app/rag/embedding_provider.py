from __future__ import annotations

from typing import Protocol

import numpy as np
from openai import OpenAI

from app.core.config import Settings


class EmbeddingProviderError(RuntimeError):
    pass


class EmbeddingProvider(Protocol):
    @property
    def model_name(self) -> str: ...

    @property
    def dimensions(self) -> int: ...

    def embed_documents(self, texts: list[str]) -> np.ndarray: ...

    def embed_query(self, text: str) -> np.ndarray: ...


def normalize_rows(values: np.ndarray) -> np.ndarray:
    matrix = np.asarray(values, dtype=np.float32)
    if matrix.ndim == 1:
        matrix = matrix.reshape(1, -1)
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    if np.any(norms == 0):
        raise EmbeddingProviderError("Embedding API returned a zero vector.")
    return matrix / norms


class OpenAIEmbeddingProvider:
    def __init__(
        self,
        *,
        api_key: str,
        model_name: str = "text-embedding-3-small",
        dimensions: int = 512,
    ) -> None:
        if not api_key.strip():
            raise ValueError("OpenAI API key is required.")
        if dimensions <= 0:
            raise ValueError("Embedding dimensions must be positive.")
        self._client = OpenAI(api_key=api_key)
        self._model_name = model_name
        self._dimensions = dimensions

    @property
    def model_name(self) -> str:
        return self._model_name

    @property
    def dimensions(self) -> int:
        return self._dimensions

    def _embed(self, texts: list[str]) -> np.ndarray:
        if not texts:
            raise ValueError("At least one text is required for embedding.")
        try:
            response = self._client.embeddings.create(
                model=self._model_name,
                input=texts,
                dimensions=self._dimensions,
                encoding_format="float",
            )
        except Exception as error:
            raise EmbeddingProviderError(
                f"OpenAI embedding request failed: {error}"
            ) from error
        if len(response.data) != len(texts):
            raise EmbeddingProviderError(
                "OpenAI returned an unexpected number of embeddings."
            )
        ordered = sorted(response.data, key=lambda item: item.index)
        return normalize_rows(
            np.asarray([item.embedding for item in ordered], dtype=np.float32)
        )

    def embed_documents(self, texts: list[str]) -> np.ndarray:
        return self._embed(texts)

    def embed_query(self, text: str) -> np.ndarray:
        if not text.strip():
            raise ValueError("Query must not be blank.")
        return self._embed([text])[0]


def create_openai_embedding_provider(
    settings: Settings,
) -> OpenAIEmbeddingProvider:
    if settings.openai_api_key is None:
        raise ValueError(
            "OPENAI_API_KEY is required to build or query the policy index."
        )
    return OpenAIEmbeddingProvider(
        api_key=settings.openai_api_key.get_secret_value(),
        model_name=settings.openai_embedding_model,
        dimensions=settings.openai_embedding_dimensions,
    )
