from __future__ import annotations

from datetime import datetime
from typing import Any
from urllib.parse import quote

import httpx

from app.core.config import Settings
from app.core.auth_context import get_user_authorization
from app.schemas.assistant_tools import InventoryResponse, ProductSummary
from app.schemas.transaction import Transaction


class BillixApiError(RuntimeError):
    pass


class BillixClient:

    def __init__(self, settings: Settings) -> None:
        self._base_url = settings.spring_base_url.rstrip("/")
        self._timeout = settings.request_timeout_seconds

    def _authorization_headers(self) -> dict[str, str]:
        return {"Authorization": get_user_authorization()}

    async def _get_json(
        self,
        path: str,
        *,
        params: dict[str, Any] | None = None,
    ) -> Any:
        url = f"{self._base_url}{path}"

        try:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                response = await client.get(
                    url,
                    headers=self._authorization_headers(),
                    params=params,
                )
                response.raise_for_status()
        except httpx.HTTPStatusError as error:
            raise BillixApiError(
                "Billix API rejected the request: "
                f"{error.response.status_code} {error.response.text}"
            ) from error
        except httpx.RequestError as error:
            raise BillixApiError(
                f"Could not connect to Billix: {error}"
            ) from error

        try:
            return response.json()
        except ValueError as error:
            raise BillixApiError("Billix returned invalid JSON.") from error

    async def fetch_transactions(
        self,
        store_id: int,
        from_time: datetime,
        to_time: datetime,
    ) -> list[Transaction]:

        payload = await self._get_json(
            f"/internal/ai/stores/{store_id}/transactions",
            params={
                "from": from_time.isoformat(),
                "to": to_time.isoformat(),
            },
        )

        if not isinstance(payload, list):
            raise BillixApiError(
                "Expected the Billix API to return a list"
            )

        return [
            Transaction.model_validate(transaction)
            for transaction in payload
        ]

    async def search_products(
        self,
        *,
        store_id: int,
        query: str,
        limit: int = 5,
    ) -> list[ProductSummary]:
        encoded_query = quote(query.strip(), safe="")
        payload = await self._get_json(
            f"/api/products/search/{store_id}/{encoded_query}"
        )

        if not isinstance(payload, list):
            raise BillixApiError(
                "Expected product search to return a list."
            )

        products = [
            ProductSummary.model_validate(product)
            for product in payload
        ]
        return products[:limit]

    async def get_inventory(
        self,
        *,
        branch_id: int,
        product_id: int,
    ) -> InventoryResponse:
        payload = await self._get_json(
            f"/api/inventories/product/{product_id}/branch/{branch_id}"
        )

        if not isinstance(payload, dict):
            raise BillixApiError(
                "Expected inventory lookup to return an object."
            )

        return InventoryResponse.model_validate(payload)

    async def get_dashboard_summary(
        self,
        *,
        branch_id: int,
        period: str = "7D",
    ) -> dict[str, Any]:
        payload = await self._get_json(
            f"/api/dashboard/branch/{branch_id}",
            params={"period": period},
        )
        if not isinstance(payload, dict):
            raise BillixApiError(
                "Expected dashboard summary to return an object."
            )
        return payload
