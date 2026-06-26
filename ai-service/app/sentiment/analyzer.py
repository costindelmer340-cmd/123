from app.api.schemas import SentimentResponse
from app.rules import NEGATIVE_WORDS, POSITIVE_WORDS, contains_any


def analyze_sentiment(text: str) -> SentimentResponse:
    negative_hits = sum(1 for word in NEGATIVE_WORDS if word in text)
    positive_hits = sum(1 for word in POSITIVE_WORDS if word in text)
    if negative_hits > positive_hits:
        score = min(0.95, 0.55 + negative_hits * 0.1)
        risk_level = "HIGH" if negative_hits >= 3 else "MEDIUM"
        return SentimentResponse(
            sentiment="NEGATIVE",
            score=score,
            risk_level=risk_level,
            summary="用户情绪偏负面，建议优先安抚并给出明确处理路径。",
        )
    if positive_hits > 0 and not contains_any(text, NEGATIVE_WORDS):
        return SentimentResponse(
            sentiment="POSITIVE",
            score=min(0.92, 0.58 + positive_hits * 0.1),
            risk_level="LOW",
            summary="用户情绪偏正面，可继续保持当前服务节奏。",
        )
    return SentimentResponse(
        sentiment="NEUTRAL",
        score=0.55,
        risk_level="LOW",
        summary="用户情绪较中性，按标准流程处理即可。",
    )
