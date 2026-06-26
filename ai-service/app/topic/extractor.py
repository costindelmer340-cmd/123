from app.api.schemas import TopicResponse
from app.rules import TOPIC_RULES


def extract_topics(text: str) -> TopicResponse:
    topics: list[str] = []
    keywords: list[str] = []
    for topic, topic_keywords in TOPIC_RULES.items():
        matched = [keyword for keyword in topic_keywords if keyword in text]
        if matched:
            topics.append(topic)
            keywords.extend(matched)
    if not topics:
        topics.append("其他问题")
    return TopicResponse(
        topics=topics,
        keywords=list(dict.fromkeys(keywords)),
        summary="、".join(topics),
    )
