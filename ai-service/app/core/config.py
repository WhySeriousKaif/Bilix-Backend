from functools import lru_cache
from pathlib import Path

from pydantic import SecretStr

from pydantic_settings import(
    BaseSettings,
    SettingsConfigDict
)

class Settings(BaseSettings):
    spring_base_url: str = (
        "http://localhost:8080"
    )

    spring_api_token: SecretStr

    request_timeout_seconds: float = 20.0

    model_directory: Path = Path("data/models")

    recommendation_top_k_limit: int = 10

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

@lru_cache  
def get_settings()-> Settings:
    return Settings()