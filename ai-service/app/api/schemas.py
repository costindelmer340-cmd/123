from pydantic import BaseModel, ConfigDict, Field


def to_camel(value: str) -> str:
    parts = value.split("_")
    return parts[0] + "".join(part.capitalize() for part in parts[1:])


class ApiModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class TextRequest(ApiModel):
    text: str = Field(min_length=1)
    merchant_id: int | None = None
    business_type: str | None = None
    business_id: int | None = None


class ContextReplyRequest(TextRequest):
    order_status: str | None = None
    after_sale_status: str | None = None
    user_tone: str | None = None
    session_id: str | None = None


class IntentResponse(ApiModel):
    intent: str
    category: str
    confidence: float
    summary: str


class SentimentResponse(ApiModel):
    sentiment: str
    score: float
    risk_level: str
    summary: str


class TopicResponse(ApiModel):
    topics: list[str]
    keywords: list[str]
    summary: str


class TicketClassifyResponse(ApiModel):
    ticket_type: str
    priority: str
    category: str
    confidence: float
    due_hours: int


class ReplyResponse(ApiModel):
    reply: str
    intent: str
    confidence: float
    suggestions: list[str]


class ReviewAnalysisResponse(ApiModel):
    sentiment: str
    sentiment_score: float
    topics: list[str]
    keywords: list[str]
    risk_level: str
    summary: str
    suggestion: str
