from typing import Tuple

class SentimentResult:
    def __init__(self, sentiment: str, score: float):
        self.sentiment = sentiment
        self.score = score

class SentimentAnalyzer:
    def __init__(self):
        self.positive_words = ["好", "棒", "满意", "不错", "漂亮", "完美", "喜欢", "感谢", "谢谢", "好评", "优秀", "出色"]
        self.negative_words = ["差", "糟糕", "不满意", "垃圾", "烂", "坏", "问题", "投诉", "差评", "质量", "破损", "投诉"]
    
    def analyze(self, text: str) -> SentimentResult:
        text = text.lower()
        positive_count = sum(1 for word in self.positive_words if word in text)
        negative_count = sum(1 for word in self.negative_words if word in text)
        
        total = positive_count + negative_count
        if total == 0:
            return SentimentResult(sentiment="NEUTRAL", score=0.5)
        
        score = positive_count / total
        
        if score > 0.6:
            sentiment = "POSITIVE"
        elif score < 0.4:
            sentiment = "NEGATIVE"
        else:
            sentiment = "NEUTRAL"
        
        return SentimentResult(sentiment=sentiment, score=score)

sentiment_analyzer = SentimentAnalyzer()