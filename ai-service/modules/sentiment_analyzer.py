"""
AI引擎 - 情感分析模块
分析用户反馈的情感极性和风险等级
"""
import re
from typing import Dict, List
from dataclasses import dataclass
from enum import Enum


class SentimentType(Enum):
    """情感极性枚举"""
    POSITIVE = "positive"      # 正向
    NEUTRAL = "neutral"        # 中性
    NEGATIVE = "negative"      # 负向


class RiskLevel(Enum):
    """风险等级枚举"""
    LOW = "low"                # 低风险
    MEDIUM = "medium"          # 中风险
    HIGH = "high"              # 高风险


@dataclass
class SentimentResult:
    """情感分析结果"""
    sentiment: SentimentType
    sentiment_score: float  # 0.0 ~ 1.0, 越接近1越正向
    risk_level: RiskLevel
    risk_keywords: List[str]
    summary: str


class SentimentAnalyzer:
    """情感分析器 - 优化版"""

    def __init__(self):
        # 正向情感词（分数越高越正向，范围0.5-1.0）
        self.positive_words = {
            '好': 0.8, '不错': 0.8, '满意': 0.85, '非常满意': 1.0,
            '很满意': 0.95, '十分满意': 1.0,
            '棒': 0.85, '赞': 0.85, '优秀': 0.9, '完美': 1.0,
            '喜欢': 0.75, '漂亮': 0.75, '好看': 0.7, '实用': 0.7,
            '快': 0.65, '迅速': 0.7, '及时': 0.7,
            '负责': 0.8, '耐心': 0.8, '热心': 0.8, '专业': 0.8,
            '感谢': 0.85, '谢谢': 0.7, '感激': 0.9,
            '理解': 0.6, '接受': 0.6,
        }

        # 负向情感词（分数越低越负向，范围0.0-0.5）
        self.negative_words = {
            '差': 0.15, '很差': 0.1, '太差': 0.05, '非常差': 0.05,
            '烂': 0.05, '垃圾': 0.0, '废物': 0.0, '垃圾商品': 0.0,
            '不满意': 0.2, '失望': 0.15, '气愤': 0.05, '愤怒': 0.0,
            '糟糕': 0.1, '恶劣': 0.1, '可恶': 0.1,
            '慢': 0.3, '太慢': 0.2, '慢死了': 0.1,
            '破损': 0.3, '损坏': 0.3, '坏了': 0.3,
            '敷衍': 0.15, '推诿': 0.1, '不负责任': 0.05,
            '欺骗': 0.0, '骗人': 0.0, '假货': 0.0,
            '投诉': 0.1, '举报': 0.05, '曝光': 0.05,
            '退款': 0.4, '退货': 0.4,
            '后悔': 0.3,
        }

        # 否定词（在正向词前面会反转情感）
        self.negation_words = {'不', '没', '未', '别', '非', '无', '不想', '不要', '不会', '无法'}

        # 强化词（会放大情感强度）
        self.intensifiers = {
            '非常': 1.5, '特别': 1.5, '十分': 1.5, '超级': 1.8,
            '极其': 2.0, '太': 1.5, '真': 1.3, '简直': 1.8,
            'quite': 1.3, 'very': 1.5,
        }

        # 风险关键词
        self.risk_keywords = {
            '投诉': (1.0, 'high'), '举报': (1.0, 'high'), '曝光': (1.0, 'high'),
            '欺诈': (1.0, 'high'), '诈骗': (1.0, 'high'), '假货': (0.8, 'high'),
            '欺骗': (0.8, 'high'), '赔偿': (0.6, 'medium'), '索赔': (0.7, 'high'),
            '气愤': (0.8, 'high'), '愤怒': (0.9, 'high'),
            '差评': (0.5, 'medium'), '退款': (0.4, 'low'), '退货': (0.4, 'low'),
            '失望': (0.5, 'medium'), '垃圾': (0.9, 'high'),
        }

    def analyze(self, text: str) -> SentimentResult:
        """
        分析文本的情感

        Args:
            text: 待分析的文本

        Returns:
            SentimentResult: 情感分析结果
        """
        if not text or not text.strip():
            return SentimentResult(
                sentiment=SentimentType.NEUTRAL,
                sentiment_score=0.5,
                risk_level=RiskLevel.LOW,
                risk_keywords=[],
                summary="无法分析空文本"
            )

        text = text.strip().lower()

        # 计算情感分数
        sentiment_score = self._calculate_sentiment(text)

        # 判断情感极性
        if sentiment_score >= 0.6:
            sentiment = SentimentType.POSITIVE
        elif sentiment_score <= 0.4:
            sentiment = SentimentType.NEGATIVE
        else:
            sentiment = SentimentType.NEUTRAL

        # 计算风险等级
        risk_level, risk_keywords = self._calculate_risk(text)

        # 生成摘要
        summary = self._generate_summary(sentiment, sentiment_score, risk_level)

        return SentimentResult(
            sentiment=sentiment,
            sentiment_score=sentiment_score,
            risk_level=risk_level,
            risk_keywords=risk_keywords,
            summary=summary
        )

    def _calculate_sentiment(self, text: str) -> float:
        """计算情感分数"""
        words = self._tokenize(text)
        if not words:
            return 0.5  # 默认中性

        total_score = 0.0
        matched_count = 0

        for i, word in enumerate(words):
            # 检查是否是强化词
            intensifier = 1.0
            if i > 0 and words[i-1] in self.intensifiers:
                intensifier = self.intensifiers[words[i-1]]

            # 检查否定词
            has_negation = False
            for j in range(max(0, i-2), i):
                if words[j] in self.negation_words:
                    has_negation = True
                    break

            # 检查正向词
            if word in self.positive_words:
                score = self.positive_words[word] * intensifier
                if has_negation:
                    score = 1.0 - score  # 否定反转
                total_score += score
                matched_count += 1
                continue

            # 检查负向词
            if word in self.negative_words:
                score = self.negative_words[word] * intensifier
                if has_negation:
                    score = 1.0 - score  # 双重否定变正向
                total_score += score
                matched_count += 1

        # 计算平均分数
        if matched_count > 0:
            avg_score = total_score / matched_count
        else:
            avg_score = 0.5  # 未匹配到任何情感词，返回中性

        # 考虑感叹号和问号的情感增强
        if '！' in text or '!' in text:
            if avg_score > 0.5:
                avg_score = min(1.0, avg_score * 1.1)
            else:
                avg_score = max(0.0, avg_score * 0.9)

        return avg_score

    def _calculate_risk(self, text: str) -> tuple:
        """计算风险等级"""
        found_keywords = []
        max_risk_level = 0  # 0=low, 1=medium, 2=high

        for keyword, (score, level) in self.risk_keywords.items():
            if keyword in text:
                found_keywords.append(keyword)
                if level == 'high':
                    max_risk_level = max(max_risk_level, 2)
                elif level == 'medium':
                    max_risk_level = max(max_risk_level, 1)

        risk_map = {0: RiskLevel.LOW, 1: RiskLevel.MEDIUM, 2: RiskLevel.HIGH}
        risk_level = risk_map[max_risk_level]

        return risk_level, list(set(found_keywords))[:5]

    def _generate_summary(self, sentiment: SentimentType, score: float, risk: RiskLevel) -> str:
        """生成情感摘要"""
        sentiment_text = {
            SentimentType.POSITIVE: "正向",
            SentimentType.NEUTRAL: "中性",
            SentimentType.NEGATIVE: "负向"
        }[sentiment]

        risk_text = {
            RiskLevel.LOW: "低风险",
            RiskLevel.MEDIUM: "中风险",
            RiskLevel.HIGH: "高风险"
        }[risk]

        return f"情感{score:.0%}为{sentiment_text}，{risk_text}"

    def _tokenize(self, text: str) -> List[str]:
        """简单分词"""
        tokens = re.findall(r'[\u4e00-\u9fa5]+|[a-zA-Z]+|\d+', text)
        return tokens


# 全局实例
sentiment_analyzer = SentimentAnalyzer()
