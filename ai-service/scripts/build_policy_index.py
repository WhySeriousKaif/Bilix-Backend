import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.config import get_settings
from app.rag.embedding_provider import create_openai_embedding_provider
from app.rag.policy_retriever import PolicyRetriever


def parse_arguments() -> argparse.Namespace:
    settings = get_settings()
    parser = argparse.ArgumentParser(
        description="Build the Billix local policy retrieval index."
    )
    parser.add_argument(
        "--policy-directory",
        type=Path,
        default=settings.policy_directory,
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=settings.policy_index_path,
    )
    return parser.parse_args()


def main() -> None:
    arguments = parse_arguments()
    settings = get_settings()
    embedding_provider = create_openai_embedding_provider(settings)
    retriever = PolicyRetriever.build(
        arguments.policy_directory,
        embedding_provider=embedding_provider,
    )
    retriever.save(arguments.output)
    print("Policy index created successfully.")
    print(f"Documents: {len(list(arguments.policy_directory.glob('*.md')))}")
    print(f"Chunks: {retriever.chunk_count}")
    print(f"Embedding model: {embedding_provider.model_name}")
    print(f"Dimensions: {embedding_provider.dimensions}")
    print(f"Saved to: {arguments.output}")


if __name__ == "__main__":
    main()
