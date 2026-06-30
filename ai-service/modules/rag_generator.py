"""
AI引擎 - RAG回复生成模块
结合知识库检索和大模型生成回复
"""
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass
from enum import Enum
import json
import re


class ResponseStrategy(Enum):
    """回复策略"""
    LLM_GENERATION = "llm"      # 大模型生成
    RAG_GENERATION = "rag"      # RAG增强生成
    FAQ_MATCH = "faq"          # FAQ精确匹配
    ESCALATE = "escalate"      # 转人工
    GREETING = "greeting"      # 问候回复
    FALLBACK = "fallback"      # 兜底回复


@dataclass
class KnowledgeItem:
    """知识条目"""
    source: str  # knowledge_base, faq, policy, product
    content: str
    title: Optional[str] = None
    url: Optional[str] = None
    relevance_score: float = 0.0


@dataclass
class GenerationContext:
    """生成上下文"""
    query: str
    intent: str
    sentiment: str
    topics: List[str]
    knowledge_items: List[KnowledgeItem]
    conversation_history: List[Dict]
    metadata: Dict


@dataclass
class GeneratedResponse:
    """生成的回复"""
    content: str
    strategy: ResponseStrategy
    confidence: float
    knowledge_used: List[str]  # 使用的知识来源
    escalation: bool
    metadata: Dict


class RAGResponseGenerator:
    """RAG回复生成器"""

    def __init__(self, faq_kb=None, llm_client=None):
        self.faq_kb = faq_kb
        self.llm_client = llm_client

        self.intent_templates = {
            "物流延迟": {
                "template": "非常抱歉给您带来不便。关于您的物流延迟问题，我来为您查询一下。根据我们的物流信息，您的包裹目前正在配送途中，预计还需{time}送达。如果您比较着急，我可以帮您联系快递小哥催促一下。",
                "variables": ["time", "tracking_info"]
            },
            "退货退款": {
                "template": '了解您的需求。关于退换货问题，我来帮您解答。{details}。您可以通过以下步骤操作：1）进入订单详情页；2）点击"申请售后"；3）选择退换原因并提交；4）等待审核通过后寄回商品。请问还有什么可以帮到您的？',
                "variables": ["details"]
            },
            "商品质量": {
                "template": "非常抱歉给您带来不好的体验。关于商品质量问题，{handling}。请您提供一下商品的照片，我们会尽快为您处理。如果确认是质量问题，我们可以为您办理退换货或退款。",
                "variables": ["handling"]
            },
            "发票问题": {
                "template": '关于发票问题，我来帮您解答。{details}。您可以在"我的订单"中找到对应订单，点击"申请开票"即可。发票会在1-3个工作日内开具，电子发票会发送到您的邮箱，纸质发票会邮寄给您。',
                "variables": ["details"]
            },
            "服务态度": {
                "template": "非常抱歉听到您的不满，我们对客服团队的服务问题深感歉意。{handling}。我们会认真处理您的反馈，并加强对客服团队的培训。请问您方便告诉我们具体是哪个客服人员吗？我们会进行核实和改进。",
                "variables": ["handling"]
            },
            "一般咨询": {
                "template": "您好，感谢您的咨询。{answer}。请问还有什么可以帮到您的？",
                "variables": ["answer"]
            },
            "问候": {
                "template": "您好！欢迎来到客服中心。我是您的智能助手，很高兴为您服务！请问有什么可以帮到您的？",
                "variables": []
            },
            "感谢": {
                "template": "不客气！很高兴能帮到您。如果后续有任何问题，欢迎随时咨询。祝您生活愉快！",
                "variables": []
            }
        }

        self.fallback_responses = [
            "抱歉，我暂时无法理解您的问题。请问您可以换个方式描述一下吗？",
            "关于这个问题，我需要进一步了解情况。您可以描述得更详细一些吗？",
            "我理解您的需求了。为了更好地帮助您，建议您联系人工客服获取更专业的解答。",
            "感谢您的提问。这个问题比较特殊，让我为您转接人工客服。",
        ]

    def generate(
        self,
        context: GenerationContext
    ) -> GeneratedResponse:
        """
        生成回复
        
        优先调用大模型，失败则降级到模板回复
        """
        query = context.query
        intent = context.intent
        sentiment = context.sentiment
        knowledge_items = context.knowledge_items

        if self._should_escalate(intent, sentiment, knowledge_items):
            return self._generate_escalation_response(context)

        if intent in ["问候", "感谢"]:
            return self._generate_simple_response(context)

        # 优先调用大模型生成
        llm_response = self._generate_llm_response(context)
        if llm_response:
            return llm_response

        # 降级到FAQ匹配
        if self.faq_kb:
            faq_results = self.faq_kb.search(query, top_k=1)
            if faq_results and faq_results[0].match_score > 0.4:
                return self._generate_faq_response(context, faq_results[0])

        # 降级到意图模板
        if intent in self.intent_templates:
            return self._generate_template_response(context)

        # 兜底回复
        return self._generate_fallback_response(context)

    def _should_escalate(
        self,
        intent: str,
        sentiment: str,
        knowledge_items: List[KnowledgeItem]
    ) -> bool:
        """判断是否需要转人工"""
        if sentiment == "negative" and intent in ["服务态度", "商品质量"]:
            return True

        high_risk_keywords = ["投诉", "举报", "曝光", "赔偿", "起诉", "律师"]
        return False

    def _generate_llm_response(
        self,
        context: GenerationContext
    ) -> Optional[GeneratedResponse]:
        """调用大模型生成回复"""
        if not self.llm_client or not self.llm_client.is_configured():
            return None

        try:
            response = self.llm_client.generate_response(
                query=context.query,
                intent=context.intent,
                sentiment=context.sentiment,
                knowledge_items=context.knowledge_items,
                conversation_history=context.conversation_history,
                metadata=context.metadata
            )

            if response and response.strip():
                return GeneratedResponse(
                    content=response.strip(),
                    strategy=ResponseStrategy.LLM_GENERATION,
                    confidence=0.95,
                    knowledge_used=[item["source"] for item in context.knowledge_items[:3]] if context.knowledge_items else [],
                    escalation=False,
                    metadata={"intent": context.intent, "topics": context.topics, "llm_used": True}
                )
        except Exception as e:
            pass

        return None

    def _generate_rag_response(
        self,
        context: GenerationContext
    ) -> GeneratedResponse:
        """RAG增强生成"""
        intent = context.intent
        knowledge_items = context.knowledge_items

        template = self.intent_templates.get(intent, self.intent_templates["一般咨询"])
        variables = self._extract_variables(context, knowledge_items)
        content = template["template"].format(**variables)

        if knowledge_items:
            context_info = self._format_knowledge_context(knowledge_items)
            content = f"{content}\n\n{context_info}"

        return GeneratedResponse(
            content=content,
            strategy=ResponseStrategy.RAG_GENERATION,
            confidence=0.85,
            knowledge_used=[item["source"] for item in knowledge_items[:3]],
            escalation=False,
            metadata={"intent": intent, "topics": context.topics}
        )

    def _generate_faq_response(
        self,
        context: GenerationContext,
        faq_result
    ) -> GeneratedResponse:
        """FAQ匹配回复"""
        faq = faq_result.faq

        prefixes = [
            "根据您的问题，我找到了相关信息：",
            "这个问题我可以帮您解答：",
            "关于您提到的，我这里有相关的说明："
        ]

        content = f"{prefixes[0]}\n\n{faq.answer}"

        return GeneratedResponse(
            content=content,
            strategy=ResponseStrategy.FAQ_MATCH,
            confidence=faq_result.match_score,
            knowledge_used=[f"faq:{faq.id}"],
            escalation=False,
            metadata={"faq_id": faq.id, "category": faq.category}
        )

    def _generate_escalation_response(
        self,
        context: GenerationContext
    ) -> GeneratedResponse:
        """转人工回复"""
        content = "我理解您的问题比较特殊，为了给您提供更好的帮助，我将为您转接人工客服。请稍等，人工客服将尽快接入。"

        if context.sentiment == "negative":
            content = "非常抱歉给您带来不好的体验。您的问题我会认真记录，并为您转接专业的人工客服处理。请您稍等，感谢您的理解和支持。"

        return GeneratedResponse(
            content=content,
            strategy=ResponseStrategy.ESCALATE,
            confidence=1.0,
            knowledge_used=[],
            escalation=True,
            metadata={"reason": "high_risk_case"}
        )

    def _generate_simple_response(
        self,
        context: GenerationContext
    ) -> GeneratedResponse:
        """简单回复（问候/感谢）"""
        intent = context.intent
        template = self.intent_templates.get(intent, self.intent_templates["一般咨询"])

        return GeneratedResponse(
            content=template["template"].format(**{}),
            strategy=ResponseStrategy.GREETING,
            confidence=1.0,
            knowledge_used=[],
            escalation=False,
            metadata={"intent": intent}
        )

    def _generate_template_response(
        self,
        context: GenerationContext
    ) -> GeneratedResponse:
        """意图模板回复"""
        intent = context.intent
        knowledge_items = context.knowledge_items

        template = self.intent_templates.get(intent, self.intent_templates["一般咨询"])
        variables = self._extract_variables(context, knowledge_items)
        content = template["template"].format(**variables)

        if knowledge_items:
            context_info = self._format_knowledge_context(knowledge_items)
            content = f"{content}\n\n{context_info}"

        return GeneratedResponse(
            content=content,
            strategy=ResponseStrategy.RAG_GENERATION,
            confidence=0.85,
            knowledge_used=[item["source"] for item in knowledge_items[:3]] if knowledge_items else [],
            escalation=False,
            metadata={"intent": intent, "topics": context.topics}
        )

    def _generate_fallback_response(
        self,
        context: GenerationContext
    ) -> GeneratedResponse:
        """兜底回复"""
        import random
        content = random.choice(self.fallback_responses)

        return GeneratedResponse(
            content=content,
            strategy=ResponseStrategy.FALLBACK,
            confidence=0.5,
            knowledge_used=[],
            escalation=False,
            metadata={"fallback": True}
        )

    def _extract_variables(
        self,
        context: GenerationContext,
        knowledge_items: List[Dict]
    ) -> Dict[str, str]:
        """从上下文中提取模板变量"""
        variables = {}
        intent = context.intent

        if intent == "物流延迟":
            variables["time"] = "1-2天"
            if knowledge_items:
                variables["tracking_info"] = "物流信息显示正在转运中"
            else:
                variables["tracking_info"] = "建议您稍后刷新查看最新物流状态"

        elif intent == "一般咨询":
            if knowledge_items:
                variables["answer"] = knowledge_items[0].get("content", "")[:100]
            else:
                variables["answer"] = "如果您有具体的问题，请告诉我，我会尽力为您解答"

        else:
            if knowledge_items:
                variables["details"] = knowledge_items[0].get("content", "")[:50]
            else:
                variables["details"] = "请允许我为您查询相关信息"
            variables["handling"] = "请您详细描述一下情况"

        return variables

    def _format_knowledge_context(
        self,
        knowledge_items: List[Dict]
    ) -> str:
        """格式化知识上下文"""
        if not knowledge_items:
            return ""

        contexts = []
        for item in knowledge_items[:3]:
            title = item.get("title", "")
            content = item.get("content", "")
            if title:
                contexts.append(f"【{title}】{content[:80]}...")
            else:
                contexts.append(content[:80] + "...")

        return "参考信息：\n" + "\n".join(contexts)


# 全局实例（未配置LLM时使用模板回复）
rag_generator = RAGResponseGenerator()