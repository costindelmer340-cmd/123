from app.api.schemas import IntentResponse
from app.rules import INTENT_RULES, contains_any


def classify_intent(text: str) -> IntentResponse:
    for intent, category, keywords in INTENT_RULES:
        if contains_any(text, keywords):
            return IntentResponse(
                intent=intent,
                category=category,
                confidence=0.88,
                summary=f"用户意图识别为{category}。",
            )
    return IntentResponse(
        intent="GENERAL_CONSULT",
        category="普通咨询",
        confidence=0.62,
        summary="暂未命中特定售后意图，按普通咨询处理。",
    )
