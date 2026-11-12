from __future__ import annotations

import re
from pathlib import Path

import joblib
import numpy as np

from app.rag.embedding_provider import EmbeddingProvider
from app.schemas.policy import PolicyChunk, PolicyCitation, PolicySearchResult


HEADING_PATTERN = re.compile(r"^(#{1,6})\s+(.+?)\s*$")
INDEX_VERSION = 2


def _split_long_text(
    text: str,
    *,
    maximum_characters: int = 900,
    overlap_characters: int = 120,
) -> list[str]:
    cleaned = " ".join(text.split())
    if not cleaned:
        return []
    if len(cleaned) <= maximum_characters:
        return [cleaned]

    chunks: list[str] = []
    start = 0
    while start < len(cleaned):
        end = min(start + maximum_characters, len(cleaned))
        if end < len(cleaned):
            sentence_end = cleaned.rfind(". ", start, end)
            if sentence_end > start + maximum_characters // 2:
                end = sentence_end + 1
        chunks.append(cleaned[start:end].strip())
        if end == len(cleaned):
            break
        start = max(end - overlap_characters, start + 1)
    return chunks


def chunk_markdown(document_path: Path) -> list[PolicyChunk]:
    text = document_path.read_text(encoding="utf-8")
    document_title = document_path.stem.replace("-", " ").title()
    current_section = "General"
    section_lines: list[str] = []
    sections: list[tuple[str, str]] = []

    def flush_section() -> None:
        content = "\n".join(section_lines).strip()
        if content:
            sections.append((current_section, content))
        section_lines.clear()

    for raw_line in text.splitlines():
        match = HEADING_PATTERN.match(raw_line.strip())
        if match:
            flush_section()
            level = len(match.group(1))
            heading = match.group(2).strip()
            if level == 1:
                document_title = heading
                current_section = "General"
            else:
                current_section = heading
            continue
        section_lines.append(raw_line)
    flush_section()

    chunks: list[PolicyChunk] = []
    chunk_number = 1
    for section, content in sections:
        for part in _split_long_text(content):
            chunks.append(
                PolicyChunk(
                    chunk_id=f"{document_path.stem}-{chunk_number}",
                    document=document_path.name,
                    title=document_title,
                    section=section,
                    content=part,
                )
            )
            chunk_number += 1
    return chunks


class PolicyRetriever:
    def __init__(
        self,
        *,
        chunks: list[PolicyChunk],
        document_embeddings: np.ndarray,
        embedding_provider: EmbeddingProvider,
    ) -> None:
        if not chunks:
            raise ValueError("Policy index must contain at least one chunk.")
        matrix = np.asarray(document_embeddings, dtype=np.float32)
        if matrix.ndim != 2 or matrix.shape[0] != len(chunks):
            raise ValueError("Policy embeddings do not match policy chunks.")
        if matrix.shape[1] != embedding_provider.dimensions:
            raise ValueError("Policy embedding dimensions do not match provider.")
        self._chunks = chunks
        self._document_embeddings = matrix
        self._embedding_provider = embedding_provider

    @classmethod
    def build(
        cls,
        policy_directory: Path,
        *,
        embedding_provider: EmbeddingProvider,
    ) -> "PolicyRetriever":
        if not policy_directory.exists():
            raise FileNotFoundError(
                f"Policy directory does not exist: {policy_directory}"
            )
        chunks: list[PolicyChunk] = []
        for document_path in sorted(policy_directory.glob("*.md")):
            chunks.extend(chunk_markdown(document_path))
        if not chunks:
            raise ValueError(
                f"No Markdown policy documents found in {policy_directory}."
            )

        searchable_text = [
            f"Title: {chunk.title}\nSection: {chunk.section}\n{chunk.content}"
            for chunk in chunks
        ]
        embeddings = embedding_provider.embed_documents(searchable_text)
        return cls(
            chunks=chunks,
            document_embeddings=embeddings,
            embedding_provider=embedding_provider,
        )

    def save(self, index_path: Path) -> None:
        index_path.parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(
            {
                "index_version": INDEX_VERSION,
                "embedding_model": self._embedding_provider.model_name,
                "embedding_dimensions": self._embedding_provider.dimensions,
                "chunks": [chunk.model_dump() for chunk in self._chunks],
                "document_embeddings": self._document_embeddings,
            },
            index_path,
        )

    @classmethod
    def load(
        cls,
        index_path: Path,
        *,
        embedding_provider: EmbeddingProvider,
    ) -> "PolicyRetriever":
        if not index_path.exists():
            raise FileNotFoundError(f"Policy index does not exist: {index_path}")
        artifact = joblib.load(index_path)
        if artifact.get("index_version") != INDEX_VERSION:
            raise ValueError("Policy index version is incompatible; rebuild it.")
        if artifact.get("embedding_model") != embedding_provider.model_name:
            raise ValueError("Policy index model differs from configured model.")
        if artifact.get("embedding_dimensions") != embedding_provider.dimensions:
            raise ValueError("Policy index dimensions differ; rebuild it.")
        return cls(
            chunks=[PolicyChunk.model_validate(item) for item in artifact["chunks"]],
            document_embeddings=artifact["document_embeddings"],
            embedding_provider=embedding_provider,
        )

    @property
    def chunk_count(self) -> int:
        return len(self._chunks)

    def search(
        self,
        query: str,
        *,
        top_k: int = 3,
        minimum_score: float = 0.20,
    ) -> PolicySearchResult:
        cleaned_query = " ".join(query.split())
        if len(cleaned_query) < 2:
            raise ValueError("Policy query must contain at least two characters.")
        if not 1 <= top_k <= 10:
            raise ValueError("top_k must be between 1 and 10.")

        query_embedding = self._embedding_provider.embed_query(cleaned_query)
        scores = self._document_embeddings @ query_embedding
        ranked_indices = np.argsort(scores)[::-1]
        citations: list[PolicyCitation] = []

        for index in ranked_indices:
            score = float(scores[index])
            if score < minimum_score:
                continue
            chunk = self._chunks[int(index)]
            citations.append(
                PolicyCitation(
                    chunk_id=chunk.chunk_id,
                    document=chunk.document,
                    title=chunk.title,
                    section=chunk.section,
                    score=round(max(0.0, min(1.0, score)), 4),
                    excerpt=chunk.content,
                )
            )
            if len(citations) == top_k:
                break
        return PolicySearchResult(query=cleaned_query, citations=citations)
