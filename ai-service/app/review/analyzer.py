from app.api.schemas import ReviewAnalysisResponse
from app.llm_client import llm_client
from app.rules import NEGATIVE_WORDS, POSITIVE_WORDS, contains_any, TOPIC_RULES


def analyze_review(text: str) -> ReviewAnalysisResponse:
    llm_result = llm_client.analyze_review(text)
    
    if llm_result and isinstance(llm_result, dict):
        return ReviewAnalysisResponse(
            sentiment=llm_result.get("sentiment", "NEUTRAL"),
            sentiment_score=llm_result.get("sentiment_score", 0.5),
            topics=llm_result.get("topics", []),
            keywords=llm_result.get("keywords", []),
            risk_level=llm_result.get("risk_level", "LOW"),
            summary=llm_result.get("summary", ""),
            suggestion=llm_result.get("suggestion", "")
        )
    
    return fallback_analyze(text)


def fallback_analyze(text: str) -> ReviewAnalysisResponse:
    negative_hits = sum(1 for word in NEGATIVE_WORDS if word in text)
    positive_hits = sum(1 for word in POSITIVE_WORDS if word in text)
    
    topics: list[str] = []
    keywords: list[str] = []
    for topic, topic_keywords in TOPIC_RULES.items():
        matched = [keyword for keyword in topic_keywords if keyword in text]
        if matched:
            topics.append(topic)
            keywords.extend(matched)
    
    if not topics:
        topics.append("其他问题")
    
    if negative_hits > positive_hits:
        sentiment = "NEGATIVE"
        score = min(0.95, 0.55 + negative_hits * 0.1)
        risk_level = "HIGH" if negative_hits >= 3 else "MEDIUM"
        summary = "用户情绪偏负面，建议优先安抚并给出明确处理路径。"
        suggestion = "建议在24小时内联系用户核实问题，提供换货或补偿方案。"
    elif positive_hits > 0 and not contains_any(text, NEGATIVE_WORDS):
        sentiment = "POSITIVE"
        score = min(0.92, 0.58 + positive_hits * 0.1)
        risk_level = "LOW"
        summary = "用户情绪偏正面，可继续保持当前服务节奏。"
        suggestion = "建议标记为低风险评价，用于服务质量复盘。"
    else:
        sentiment = "NEUTRAL"
        score = 0.55
        risk_level = "LOW"
        summary = "用户情绪较中性，按标准流程处理即可。"
        suggestion = "建议关注后续用户反馈，保持正常服务响应。"
    
    return ReviewAnalysisResponse(
        sentiment=sentiment,
        sentiment_score=score,
        topics=topics,
        keywords=list(dict.fromkeys(keywords)),
        risk_level=risk_level,
        summary=summary,
        suggestion=suggestion
    )