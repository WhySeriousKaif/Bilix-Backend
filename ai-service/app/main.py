from contextlib import asynccontextmanager
import logging
from pathlib import Path
from typing import AsyncIterator

from fastapi import (
    FastAPI,
    HTTPException,
    Request,
)
from fastapi.responses import JSONResponse

from app.core.config import get_settings
from app.core.auth_context import (
    reset_user_authorization,
    set_user_authorization,
)
from app.clients.billix_client import BillixClient
from app.rag.embedding_provider import create_openai_embedding_provider
from app.rag.policy_retriever import PolicyRetriever
from app.schemas.assistant import (
    AssistantHealthResponse,
    AssistantRequest,
    AssistantResponse,
)
from app.schemas.recommendation import (
    HealthResponse,
    RecommendationRequest,
    RecommendationResponse,
)
from app.services.recommendation_service import (
    RecommendationService,
)
from app.services.assistant_service import AssistantService
from app.services.assistant_tools import AssistantTools


logger = logging.getLogger(__name__)


def create_app(
    model_directory: Path | None = None,
    assistant_service: AssistantService | None = None,
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

        app.state.policy_index_loaded = False
        app.state.assistant_service = assistant_service

        if app.state.assistant_service is None:
            if settings.openai_api_key is None:
                logger.info(
                    "Assistant disabled because OPENAI_API_KEY is not configured."
                )
            elif not settings.policy_index_path.exists():
                logger.info(
                    "Assistant disabled because the policy index does not exist: %s",
                    settings.policy_index_path,
                )
            else:
                try:
                    embedding_provider = create_openai_embedding_provider(
                        settings
                    )
                    policy_retriever = PolicyRetriever.load(
                        settings.policy_index_path,
                        embedding_provider=embedding_provider,
                    )
                    tools = AssistantTools(
                        billix_client=BillixClient(settings),
                        recommendation_service=(
                            app.state.recommendation_service
                        ),
                        policy_retriever=policy_retriever,
                    )
                    app.state.assistant_service = (
                        AssistantService.from_api_key(
                            api_key=(
                                settings.openai_api_key.get_secret_value()
                            ),
                            tools=tools,
                            model=settings.openai_chat_model,
                            max_tool_rounds=(
                                settings.assistant_max_tool_rounds
                            ),
                        )
                    )
                    app.state.policy_index_loaded = True
                except Exception:
                    logger.exception(
                        "Assistant initialization failed; recommendation API remains available."
                    )
        else:
            app.state.policy_index_loaded = True

        yield

    application = FastAPI(
        title="Billix AI Service",
        version="1.0.0",
        lifespan=lifespan,
    )

    @application.middleware("http")
    async def verify_service_key(request: Request, call_next):
        settings = get_settings()
        expected_key = settings.ai_service_api_key.get_secret_value()
        public_paths = {"/health", "/assistant/health", "/docs", "/openapi.json"}
        if (
            expected_key
            and request.url.path not in public_paths
            and request.headers.get("X-AI-Service-Key") != expected_key
        ):
            return JSONResponse(
                status_code=401,
                content={"detail": "Invalid AI service key."},
            )
        return await call_next(request)

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

    @application.get(
        "/assistant/health",
        response_model=AssistantHealthResponse,
    )
    async def assistant_health(
        request: Request,
    ) -> AssistantHealthResponse:
        loaded = (
            getattr(
                request.app.state,
                "assistant_service",
                None,
            )
            is not None
        )
        return AssistantHealthResponse(
            status="UP" if loaded else "DOWN",
            assistant_loaded=loaded,
            policy_index_loaded=bool(
                getattr(
                    request.app.state,
                    "policy_index_loaded",
                    False,
                )
            ),
        )

    @application.post(
        "/assistant/chat",
        response_model=AssistantResponse,
    )
    async def assistant_chat(
        payload: AssistantRequest,
        request: Request,
    ) -> AssistantResponse:
        authorization = request.headers.get("Authorization", "").strip()
        if not authorization.startswith("Bearer ") or not authorization[7:].strip():
            raise HTTPException(
                status_code=401,
                detail="A signed-in Billix user is required.",
            )

        service = get_assistant_service(
            request
        )
        context_token = set_user_authorization(authorization)
        try:
            return await service.answer(payload)
        except RuntimeError as error:
            raise HTTPException(
                status_code=503,
                detail=str(error),
            ) from error
        finally:
            reset_user_authorization(context_token)

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


def get_assistant_service(
    request: Request,
) -> AssistantService:
    service = getattr(
        request.app.state,
        "assistant_service",
        None,
    )
    if service is None:
        raise HTTPException(
            status_code=503,
            detail=(
                "Assistant is unavailable. Configure OPENAI_API_KEY "
                "and build the policy index."
            ),
        )
    return service


app = create_app()
