"""
AI引擎 - 多轮对话管理模块
维护会话上下文和历史记录
"""
from typing import List, Dict, Optional
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
import json
import hashlib


class MessageRole(Enum):
    """消息角色"""
    USER = "user"
    ASSISTANT = "assistant"
    SYSTEM = "system"


@dataclass
class Message:
    """对话消息"""
    role: MessageRole
    content: str
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    metadata: Dict = field(default_factory=dict)

    def to_dict(self) -> dict:
        return {
            "role": self.role.value,
            "content": self.content,
            "timestamp": self.timestamp,
            "metadata": self.metadata
        }

    @classmethod
    def from_dict(cls, data: dict) -> 'Message':
        return cls(
            role=MessageRole(data.get("role", "user")),
            content=data.get("content", ""),
            timestamp=data.get("timestamp", datetime.now().isoformat()),
            metadata=data.get("metadata", {})
        )


@dataclass
class ConversationContext:
    """会话上下文"""
    conversation_id: str
    merchant_id: int
    user_id: int
    order_id: Optional[int] = None
    messages: List[Message] = field(default_factory=list)
    current_intent: Optional[str] = None
    current_topic: Optional[str] = None
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())
    updated_at: str = field(default_factory=lambda: datetime.now().isoformat())
    metadata: Dict = field(default_factory=dict)

    def add_message(self, role: MessageRole, content: str, metadata: Dict = None):
        """添加消息"""
        message = Message(
            role=role,
            content=content,
            metadata=metadata or {}
        )
        self.messages.append(message)
        self.updated_at = datetime.now().isoformat()

    def get_context_messages(self, max_messages: int = 10) -> List[Message]:
        """获取上下文消息（用于发送给LLM）"""
        return self.messages[-max_messages:]

    def clear_history(self):
        """清空历史"""
        self.messages = []
        self.updated_at = datetime.now().isoformat()

    def to_dict(self) -> dict:
        return {
            "conversation_id": self.conversation_id,
            "merchant_id": self.merchant_id,
            "user_id": self.user_id,
            "order_id": self.order_id,
            "messages": [m.to_dict() for m in self.messages],
            "current_intent": self.current_intent,
            "current_topic": self.current_topic,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "metadata": self.metadata
        }

    @classmethod
    def from_dict(cls, data: dict) -> 'ConversationContext':
        return cls(
            conversation_id=data.get("conversation_id", ""),
            merchant_id=data.get("merchant_id", 0),
            user_id=data.get("user_id", 0),
            order_id=data.get("order_id"),
            messages=[Message.from_dict(m) for m in data.get("messages", [])],
            current_intent=data.get("current_intent"),
            current_topic=data.get("current_topic"),
            created_at=data.get("created_at", datetime.now().isoformat()),
            updated_at=data.get("updated_at", datetime.now().isoformat()),
            metadata=data.get("metadata", {})
        )


class ConversationManager:
    """会话管理器"""

    def __init__(self):
        # 内存存储会话
        self.conversations: Dict[str, ConversationContext] = {}

        # 会话过期时间（秒）
        self.session_expire_seconds = 3600

        # 最大上下文消息数
        self.max_context_messages = 10

    def create_conversation(
        self,
        conversation_id: str,
        merchant_id: int,
        user_id: int,
        order_id: Optional[int] = None,
        metadata: Dict = None
    ) -> ConversationContext:
        """
        创建新会话

        Args:
            conversation_id: 会话ID
            merchant_id: 商家ID
            user_id: 用户ID
            order_id: 订单ID（可选）
            metadata: 元数据

        Returns:
            ConversationContext: 会话上下文
        """
        # 检查是否已存在
        if conversation_id in self.conversations:
            return self.conversations[conversation_id]

        # 创建新会话
        context = ConversationContext(
            conversation_id=conversation_id,
            merchant_id=merchant_id,
            user_id=user_id,
            order_id=order_id,
            metadata=metadata or {}
        )

        self.conversations[conversation_id] = context
        return context

    def get_conversation(self, conversation_id: str) -> Optional[ConversationContext]:
        """
        获取会话

        Args:
            conversation_id: 会话ID

        Returns:
            ConversationContext: 会话上下文，不存在则返回None
        """
        return self.conversations.get(conversation_id)

    def add_user_message(
        self,
        conversation_id: str,
        content: str,
        metadata: Dict = None
    ) -> Optional[Message]:
        """
        添加用户消息

        Args:
            conversation_id: 会话ID
            content: 消息内容
            metadata: 元数据

        Returns:
            Message: 创建的消息对象
        """
        context = self.get_conversation(conversation_id)
        if not context:
            return None

        message = Message(
            role=MessageRole.USER,
            content=content,
            metadata=metadata or {}
        )
        context.messages.append(message)
        context.updated_at = datetime.now().isoformat()

        # 限制消息数量
        if len(context.messages) > self.max_context_messages * 2:
            # 保留系统消息和最近的对话
            context.messages = context.messages[-self.max_context_messages * 2:]

        return message

    def add_assistant_message(
        self,
        conversation_id: str,
        content: str,
        metadata: Dict = None
    ) -> Optional[Message]:
        """
        添加助手消息

        Args:
            conversation_id: 会话ID
            content: 消息内容
            metadata: 元数据

        Returns:
            Message: 创建的消息对象
        """
        context = self.get_conversation(conversation_id)
        if not context:
            return None

        message = Message(
            role=MessageRole.ASSISTANT,
            content=content,
            metadata=metadata or {}
        )
        context.messages.append(message)
        context.updated_at = datetime.now().isoformat()

        return message

    def get_conversation_history(
        self,
        conversation_id: str,
        max_messages: int = None
    ) -> List[Message]:
        """
        获取对话历史

        Args:
            conversation_id: 会话ID
            max_messages: 最大消息数（最近N条）

        Returns:
            List[Message]: 消息列表
        """
        context = self.get_conversation(conversation_id)
        if not context:
            return []

        messages = context.messages
        if max_messages:
            messages = messages[-max_messages:]

        return messages

    def update_context(
        self,
        conversation_id: str,
        intent: str = None,
        topic: str = None,
        metadata: Dict = None
    ) -> bool:
        """
        更新会话上下文信息

        Args:
            conversation_id: 会话ID
            intent: 当前意图
            topic: 当前主题
            metadata: 其他元数据

        Returns:
            bool: 是否成功
        """
        context = self.get_conversation(conversation_id)
        if not context:
            return False

        if intent:
            context.current_intent = intent
        if topic:
            context.current_topic = topic
        if metadata:
            context.metadata.update(metadata)

        context.updated_at = datetime.now().isoformat()
        return True

    def clear_conversation(self, conversation_id: str) -> bool:
        """
        清空会话历史

        Args:
            conversation_id: 会话ID

        Returns:
            bool: 是否成功
        """
        context = self.get_conversation(conversation_id)
        if not context:
            return False

        context.clear_history()
        return True

    def delete_conversation(self, conversation_id: str) -> bool:
        """
        删除会话

        Args:
            conversation_id: 会话ID

        Returns:
            bool: 是否成功
        """
        if conversation_id in self.conversations:
            del self.conversations[conversation_id]
            return True
        return False

    def get_user_conversations(self, user_id: int) -> List[ConversationContext]:
        """
        获取用户的所有会话

        Args:
            user_id: 用户ID

        Returns:
            List[ConversationContext]: 会话列表
        """
        return [
            ctx for ctx in self.conversations.values()
            if ctx.user_id == user_id
        ]

    @staticmethod
    def generate_conversation_id(merchant_id: int, user_id: int) -> str:
        """
        生成会话ID

        Args:
            merchant_id: 商家ID
            user_id: 用户ID

        Returns:
            str: 会话ID
        """
        data = f"{merchant_id}:{user_id}:{datetime.now().isoformat()}"
        return hashlib.md5(data.encode()).hexdigest()[:16]


# 全局实例
conversation_manager = ConversationManager()
