from contextlib import asynccontextmanager
from pathlib import Path
from typing import AsyncIterator

from fastapi import (
    FastAPI,
    HTTPException,
    Request,
)

from app.core.config import get_settings
from app.schemas.recommendation import (
    HealthResponse,
    RecommendationRequest,
    RecommendationResponse,
)
from app.services.recommendation_service import (
    RecommendationService,
)


def create_app(
    model_directory: Path | None = None,
) -> FastAPI:
    @asynccontextmanager
    async def lifespan(
        app: FastAPI,
    ) -> AsyncIterator[None]:
        settings = get_settings()

        selected_directory = (
            model_directory
            if model_directory is not None
            else settings.model_directory
        )

        app.state.recommendation_service = (
            RecommendationService.from_directory(
                selected_directory
            )
        )

        yield

    application = FastAPI(
        title="Billix AI Service",
        version="1.0.0",
        lifespan=lifespan,
    )

    @application.get(
        "/health",
        response_model=HealthResponse,
    )
    async def health(
        request: Request,
    ) -> HealthResponse:
        service = getattr(
            request.app.state,
            "recommendation_service",
            None,
        )

        return HealthResponse(
            status=(
                "UP"
                if service is not None
                else "DOWN"
            ),
            model_loaded=service is not None,
        )

    @application.get("/model/info")
    async def model_info(
        request: Request,
    ) -> dict:
        service = get_recommendation_service(
            request
        )

        return service.metadata

    @application.post(
        "/recommendations",
        response_model=RecommendationResponse,
    )
    async def recommendations(
        payload: RecommendationRequest,
        request: Request,
    ) -> RecommendationResponse:
        settings = get_settings()

        if (
            payload.top_k
            > settings.recommendation_top_k_limit
        ):
            raise HTTPException(
                status_code=400,
                detail=(
                    "top_k exceeds the configured "
                    "recommendation limit."
                ),
            )

        service = get_recommendation_service(
            request
        )

        results = service.recommend(
            product_ids=payload.product_ids,
            top_k=payload.top_k,
        )

        return RecommendationResponse(
            store_id=payload.store_id,
            recommendations=results,
            model_version=service.model_version,
        )

    return application


def get_recommendation_service(
    request: Request,
) -> RecommendationService:
    service = getattr(
        request.app.state,
        "recommendation_service",
        None,
    )

    if service is None:
        raise HTTPException(
            status_code=503,
            detail=(
                "Recommendation model "
                "is not available."
            ),
        )

    return service


app = create_app()