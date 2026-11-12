import argparse
import asyncio
from datetime import datetime
from pathlib import Path
import sys

# Ensure ai-service root is in sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.clients.billix_client import BillixClient
from app.core.config import get_settings
from app.data.normalizer import normalize_transactions


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Fetch and normalize completed Billix "
            "transactions."
        )
    )

    parser.add_argument(
        "--store-id",
        type=int,
        required=True,
    )

    parser.add_argument(
        "--from-time",
        type=datetime.fromisoformat,
        required=True,
    )

    parser.add_argument(
        "--to-time",
        type=datetime.fromisoformat,
        required=True,
    )

    return parser.parse_args()


async def run() -> None:
    arguments = parse_arguments()

    settings = get_settings()
    client = BillixClient(settings)

    transactions = await client.fetch_transactions(
        store_id=arguments.store_id,
        from_time=arguments.from_time,
        to_time=arguments.to_time,
    )

    transactions_df, items_df = (
        normalize_transactions(transactions)
    )

    output_directory = Path("data/processed")
    output_directory.mkdir(
        parents=True,
        exist_ok=True,
    )

    transactions_df.to_csv(
        output_directory / "transactions.csv",
        index=False,
    )

    items_df.to_csv(
        output_directory / "transaction_items.csv",
        index=False,
    )

    print(
        f"Fetched {len(transactions_df)} transactions"
    )

    print(
        f"Created {len(items_df)} item rows"
    )


if __name__ == "__main__":
    asyncio.run(run())