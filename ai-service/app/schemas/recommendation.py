from pydantic import BaseModel, Field


class RecommendationRequest(BaseModel):
    store_id: int = Field(gt=0)

    product_ids: list[int] = Field(
        default_factory=list,
    )

    top_k: int = Field(
        default=3,
        ge=1,
        le=10,
    )


class RecommendedProduct(BaseModel):
    product_id: int
    score: float
    confidence: float
    lift: float
    reason: str


class RecommendationResponse(BaseModel):
    store_id: int
    recommendations: list[RecommendedProduct]
    model_version: str


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool