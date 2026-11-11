from datetime import datetime

import httpx

from app.core.config import Settings
from app.schemas.transaction import Transaction


class BillixApiError(RuntimeError):
    pass


class BillixClient:

    def __init__(self, settings: Settings) -> None:
        self._base_url = settings.spring_base_url.rstrip("/")
        self._token = (
            settings.spring_api_token.get_secret_value()
        )
        self._timeout = settings.request_timeout_seconds

    async def fetch_transactions(
        self,
        store_id: int,
        from_time: datetime,
        to_time: datetime,
    ) -> list[Transaction]:

        url = (
            f"{self._base_url}/internal/ai/"
            f"stores/{store_id}/transactions"
        )

        headers = {
            "Authorization": f"Bearer {self._token}",
        }

        params = {
            "from": from_time.isoformat(),
            "to": to_time.isoformat(),
        }

        try:
            async with httpx.AsyncClient(
                timeout=self._timeout
            ) as client:
                response = await client.get(
                    url,
                    headers=headers,
                    params=params,
                )

                response.raise_for_status()

        except httpx.HTTPStatusError as error:
            raise BillixApiError(
                "Billix API rejected the request: "
                f"{error.response.status_code} "
                f"{error.response.text}"
            ) from error

        except httpx.RequestError as error:
            raise BillixApiError(
                f"Could not connect to Billix: {error}"
            ) from error

        payload = response.json()

        if not isinstance(payload, list):
            raise BillixApiError(
                "Expected the Billix API to return a list"
            )

        return [
            Transaction.model_validate(transaction)
            for transaction in payload
        ]