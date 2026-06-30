"""
AI引擎 - LLM客户端模块
封装大模型API调用，支持DeepSeek、Qwen、GLM等多种模型
"""
from typing import Optional, List, Dict, Any
from openai import OpenAI
from openai.types.chat import ChatCompletionMessage
import time
import logging

logger = logging.getLogger(__name__)


class LLMClient:
    """LLM客户端"""

    def __init__(self, config):
        self.config = config
        self.client = self._create_client()
        self.model_name = self._get_model_name()
        self.api_key = self._get_api_key()

    def _create_client(self) -> OpenAI:
        """创建OpenAI客户端"""
        provider = self.config.MODEL_PROVIDER.lower()
        
        if provider == "deepseek":
            return OpenAI(
                api_key=self.config.DEEPSEEK_API_KEY or "sk-placeholder",
                base_url=self.config.DEEPSEEK_BASE_URL
            )
        elif provider == "qwen":
            return OpenAI(
                api_key=self.config.QWEN_API_KEY or "sk-placeholder",
                base_url=self.config.QWEN_BASE_URL
            )
        elif provider == "glm":
            return OpenAI(
                api_key=self.config.GLM_API_KEY or "sk-placeholder",
                base_url=self.config.GLM_BASE_URL
            )
        elif provider == "openai":
            return OpenAI(
                api_key=self.config.OPENAI_API_KEY or "sk-placeholder",
                base_url=self.config.OPENAI_BASE_URL
            )
        else:
            raise ValueError(f"Unsupported model provider: {provider}")

    def _get_model_name(self) -> str:
        """获取模型名称"""
        provider = self.config.MODEL_PROVIDER.lower()
        
        if provider == "deepseek":
            return self.config.DEEPSEEK_MODEL
        elif provider == "qwen":
            return self.config.QWEN_MODEL
        elif provider == "glm":
            return self.config.GLM_MODEL
        elif provider == "openai":
            return self.config.OPENAI_MODEL
        else:
            return "deepseek-chat"

    def _get_api_key(self) -> Optional[str]:
        """获取API Key"""
        provider = self.config.MODEL_PROVIDER.lower()
        
        if provider == "deepseek":
            return self.config.DEEPSEEK_API_KEY
        elif provider == "qwen":
            return self.config.QWEN_API_KEY
        elif provider == "glm":
            return self.config.GLM_API_KEY
        elif provider == "openai":
            return self.config.OPENAI_API_KEY
        else:
            return None

    def is_configured(self) -> bool:
        """检查是否已配置API Key"""
        return self.api_key is not None and len(self.api_key) > 0

    def chat_completion(
        self,
        messages: List[Dict[str, str]],
        temperature: Optional[float] = None,
        max_tokens: Optional[int] = None,
        timeout: Optional[int] = None
    ) -> Optional[str]:
        """
        调用大模型进行对话补全
        
        Args:
            messages: 对话消息列表
            temperature: 温度参数
            max_tokens: 最大token数
            timeout: 超时时间
            
        Returns:
            模型回复内容，如果调用失败返回None
        """
        if not self.is_configured():
            logger.warning("LLM API key not configured, skipping LLM call")
            return None

        try:
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=messages,
                temperature=temperature or self.config.MODEL_TEMPERATURE,
                max_tokens=max_tokens or self.config.MODEL_MAX_TOKENS,
                timeout=timeout or self.config.LLM_TIMEOUT
            )
            
            if response and response.choices:
                return response.choices[0].message.content
            
            return None
            
        except Exception as e:
            logger.error(f"LLM call failed: {str(e)}")
            return None

    def generate_response(
        self,
        query: str,
        intent: str,
        sentiment: str,
        knowledge_items: List[Dict],
        conversation_history: List[Dict] = None,
        metadata: Dict = None
    ) -> Optional[str]:
        """
        生成智能回复
        
        Args:
            query: 用户查询
            intent: 用户意图
            sentiment: 情感极性
            knowledge_items: 知识库条目
            conversation_history: 对话历史
            metadata: 元数据
            
        Returns:
            生成的回复，如果失败返回None
        """
        if not self.is_configured():
            return None

        system_prompt = self._build_system_prompt(intent, sentiment, knowledge_items, metadata)
        user_prompt = self._build_user_prompt(query, conversation_history)

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ]

        return self.chat_completion(messages)

    def _build_system_prompt(
        self,
        intent: str,
        sentiment: str,
        knowledge_items: List[Dict],
        metadata: Dict = None
    ) -> str:
        """构建系统提示词"""
        prompt = f"""
你是一名专业的电商售后客服智能助手，你的任务是根据用户的问题提供友好、专业、准确的回复。

【用户意图】: {intent}
【情感分析】: {sentiment}

【知识库参考】:
"""

        if knowledge_items:
            for i, item in enumerate(knowledge_items, 1):
                title = item.get("title", "")
                content = item.get("content", "")
                if title:
                    prompt += f"{i}.【{title}】\n{content}\n\n"
                else:
                    prompt += f"{i}.{content}\n\n"
        else:
            prompt += "暂无相关知识库内容，请根据通用售后知识回答。\n\n"

        prompt += """
【回复要求】:
1. 语气友好、礼貌，使用亲切的称呼（如"亲"、"您好"）
2. 如果有知识库内容，请优先参考知识库进行回答
3. 根据用户意图提供针对性的解决方案
4. 如果是负面情感，先表达歉意，再提供解决方案
5. 回复要简洁明了，避免冗长
6. 如果问题超出知识范围，建议用户联系人工客服
7. 不要透露自己是AI模型
"""

        return prompt.strip()

    def _build_user_prompt(
        self,
        query: str,
        conversation_history: List[Dict] = None
    ) -> str:
        """构建用户提示词"""
        prompt = f"用户问题: {query}\n\n"

        if conversation_history and len(conversation_history) > 0:
            prompt += "对话历史:\n"
            for msg in conversation_history[-5:]:
                role = msg.get("role", "")
                content = msg.get("content", "")
                if role and content:
                    prompt += f"{role}: {content}\n"

        prompt += "\n请给出合适的回复:"
        return prompt.strip()


# 全局实例
from config import settings
llm_client = LLMClient(settings)