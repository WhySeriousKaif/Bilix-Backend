from pydantic import BaseModel, Field


class PolicyChunk(BaseModel):
    chunk_id: str
    document: str
    title: str
    section: str
    content: str


class PolicyCitation(BaseModel):
    chunk_id: str
    document: str
    title: str
    section: str
    score: float = Field(ge=0, le=1)
    excerpt: str


class PolicySearchResult(BaseModel):
    query: str
    citations: list[PolicyCitation]
