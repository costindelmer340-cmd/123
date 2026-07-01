from fastapi import APIRouter

from app.api.schemas import (
    ContextReplyRequest,
    IntentResponse,
    ReplyResponse,
    ReviewAnalysisResponse,
    SentimentResponse,
    TextRequest,
    TicketClassifyResponse,
    TopicResponse,
)
from app.dialog.reply import build_reply
from app.intent.classifier import classify_intent
from app.sentiment.analyzer import analyze_sentiment
from app.ticket.classifier import classify_ticket
from app.topic.extractor import extract_topics
from app.review.analyzer import analyze_review
from app.config import settings
from app.llm_client import llm_client
from app.rag.service import rag_service

router = APIRouter()


@router.post("/intent", response_model=IntentResponse)
def intent(request: TextRequest) -> IntentResponse:
    return classify_intent(request.text)


@router.post("/sentiment", response_model=SentimentResponse)
def sentiment(request: TextRequest) -> SentimentResponse:
    return analyze_sentiment(request.text)


@router.post("/topic", response_model=TopicResponse)
def topic(request: TextRequest) -> TopicResponse:
    return extract_topics(request.text)


@router.post("/ticket/classify", response_model=TicketClassifyResponse)
def ticket_classify(request: TextRequest) -> TicketClassifyResponse:
    return classify_ticket(request.text)


@router.post("/reply", response_model=ReplyResponse)
def reply(request: ContextReplyRequest) -> ReplyResponse:
    return build_reply(request)


@router.post("/review/analyze", response_model=ReviewAnalysisResponse)
def review_analyze(request: TextRequest) -> ReviewAnalysisResponse:
    return analyze_review(request.text)


@router.get("/config")
def config() -> dict[str, object]:
    provider = settings.model_provider.lower()
    model_name = getattr(settings, f"{provider}_model", "")
    base_url = getattr(settings, f"{provider}_base_url", "")
    return {
        "serviceName": settings.app_name,
        "serviceVersion": settings.app_version,
        "serviceUrl": "http://localhost:9000",
        "provider": provider,
        "modelName": model_name,
        "baseUrl": base_url,
        "apiKeyConfigured": llm_client.is_configured(),
        "maxTokens": settings.model_max_tokens,
        "timeoutSeconds": settings.llm_timeout,
        "replyMode": "真实大模型回复",
        "fallbackMode": "服务不可用时提示转人工",
    }


@router.post("/rag/sync")
def rag_sync(request: dict) -> dict:
    knowledge_items = request.get("items", [])
    if knowledge_items:
        rag_service.add_knowledge(knowledge_items)
        return {"status": "success", "count": len(knowledge_items)}
    return {"status": "success", "count": 0}


@router.post("/rag/search")
def rag_search(request: dict) -> dict:
    query = request.get("query", "")
    top_k = request.get("top_k", 3)
    merchant_id = request.get("merchant_id")
    
    results = rag_service.search_knowledge(query, top_k=top_k, merchant_id=merchant_id)
    return {"results": results}


@router.get("/rag/stats")
def rag_stats() -> dict:
    return rag_service.get_stats()
