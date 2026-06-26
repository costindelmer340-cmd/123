from fastapi import APIRouter

from app.api.schemas import (
    ContextReplyRequest,
    IntentResponse,
    ReplyResponse,
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
