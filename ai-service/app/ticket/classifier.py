from app.api.schemas import TicketClassifyResponse
from app.intent.classifier import classify_intent
from app.sentiment.analyzer import analyze_sentiment


def classify_ticket(text: str) -> TicketClassifyResponse:
    intent = classify_intent(text)
    sentiment = analyze_sentiment(text)
    priority = "HIGH" if sentiment.risk_level == "HIGH" or intent.intent in {"COMPLAINT", "REPAIR"} else "NORMAL"
    due_hours = 4 if priority == "HIGH" else 24
    return TicketClassifyResponse(
        ticket_type="AFTER_SALE" if intent.intent != "GENERAL_CONSULT" else "CONSULT",
        priority=priority,
        category=intent.category,
        confidence=max(intent.confidence, sentiment.score),
        due_hours=due_hours,
    )
