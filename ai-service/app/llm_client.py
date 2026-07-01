from __future__ import annotations

import logging
from typing import Any

from openai import OpenAI

from app.config import settings

logger = logging.getLogger(__name__)


class LLMClient:
    def __init__(self) -> None:
        self.provider = settings.model_provider.lower()
        self.api_key = self._api_key()
        self.base_url = self._base_url()
        self.model_name = self._model_name()
        self.client = OpenAI(api_key=self.api_key or "sk-placeholder", base_url=self.base_url)

    def is_configured(self) -> bool:
        return bool(self.api_key)

    def generate_after_sale_reply(
        self,
        text: str,
        intent: str,
        category: str,
        order_status: str | None = None,
        after_sale_status: str | None = None,
        user_tone: str | None = None,
        order_context: str = "",
        context_summary: str = "",
        knowledge_context: str = "",
    ) -> str | None:
        if not self.is_configured():
            return None

        system_prompt = "简洁回答，不寒暄，不客套，50字以内。优先参考知识库内容回答。"
        context = (
            f"识别意图：{intent}\n"
            f"业务分类：{category}\n"
            f"订单状态：{order_status or '未知'}\n"
            f"售后状态：{after_sale_status or '未知'}\n"
            f"用户语气：{user_tone or '未知'}"
        )
        
        if knowledge_context:
            context += f"\n\n【知识库参考】\n{knowledge_context}"
        
        if context_summary:
            context += f"\n\n{context_summary}"
        
        if order_context and not context_summary.startswith("订单信息"):
            context += f"\n\n订单详情：\n{order_context}"
        
        user_prompt = f"{context}\n\n用户问题：{text}\n\n请给出简洁回复（80字以内）。"

        try:
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                max_tokens=settings.model_max_tokens,
                timeout=settings.llm_timeout,
            )
            if response and response.choices:
                content = response.choices[0].message.content
                if content:
                    content = content.strip()
                    if len(content) > 60:
                        content = content[:60] + "..."
                    content = content.replace("\n", " ")
                return content
        except Exception as exc:
            logger.warning("LLM reply generation failed: %s", exc)
        return None

    def analyze_review(self, text: str) -> dict | None:
        if not self.is_configured():
            return None

        system_prompt = """
你是电商售后评价分析专家。请分析用户评价并返回JSON格式结果。

要求：
1. sentiment: 情感分类，只能是POSITIVE(正向)/NEGATIVE(负向)/NEUTRAL(中性)/MIXED(混合)
2. sentiment_score: 情感分数，0-1之间，越接近1越正向
3. topics: 主题列表，提取2-4个核心主题，如["商品质量", "物流速度", "客服服务"]
4. keywords: 关键词列表，提取3-6个关键信息词
5. risk_level: 风险等级，只能是HIGH(高)/MEDIUM(中)/LOW(低)/NONE(无)
6. summary: 分析摘要，50字以内
7. suggestion: 处理建议，60字以内

请严格按照JSON格式返回，不要包含其他内容。
"""

        user_prompt = f"用户评价：{text}\n\n请分析并返回JSON格式结果。"

        try:
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                max_tokens=500,
                timeout=settings.llm_timeout,
            )
            if response and response.choices:
                content = response.choices[0].message.content
                if content:
                    content = content.strip()
                    import json
                    try:
                        start = content.find("{")
                        end = content.rfind("}") + 1
                        if start >= 0 and end > start:
                            json_str = content[start:end]
                            return json.loads(json_str)
                    except Exception:
                        logger.warning("Failed to parse review analysis JSON: %s", content)
            return None
        except Exception as exc:
            logger.warning("LLM review analysis failed: %s", exc)
        return None

    def _api_key(self) -> str:
        return str(getattr(settings, f"{self.provider}_api_key", "") or "")

    def _base_url(self) -> str:
        return str(getattr(settings, f"{self.provider}_base_url", settings.deepseek_base_url))

    def _model_name(self) -> str:
        return str(getattr(settings, f"{self.provider}_model", settings.deepseek_model))


llm_client = LLMClient()
