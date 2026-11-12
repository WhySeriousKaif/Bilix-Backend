from functools import lru_cache
from pathlib import Path

from pydantic import SecretStr, field_validator

from pydantic_settings import(
    BaseSettings,
    SettingsConfigDict
)

class Settings(BaseSettings):
    spring_base_url: str = (
        "http://localhost:8080"
    )

    spring_api_token: SecretStr = SecretStr("")

    ai_service_api_key: SecretStr = SecretStr("")

    request_timeout_seconds: float = 20.0

    model_directory: Path = Path("data/models")

    recommendation_top_k_limit: int = 10

    policy_directory: Path = Path("data/policies")

    policy_index_path: Path = Path("data/rag/policy_index.joblib")

    policy_search_top_k_limit: int = 5

    openai_api_key: SecretStr | None = None

    openai_chat_model: str = "gpt-6-luna"

    openai_embedding_model: str = "text-embedding-3-small"

    openai_embedding_dimensions: int = 512

    assistant_max_tool_rounds: int = 4

    @field_validator("spring_base_url")
    @classmethod
    def normalize_spring_base_url(cls, value: str) -> str:
        if value.startswith(("http://", "https://")):
            return value.rstrip("/")
        return f"https://{value.rstrip('/')}"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

@lru_cache  
def get_settings()-> Settings:
    return Settings()
