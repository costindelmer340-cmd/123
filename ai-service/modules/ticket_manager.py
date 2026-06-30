"""
AI引擎 - 工单管理模块
实现工单的创建、状态流转、分配和关闭（使用MySQL数据库）
"""
from typing import List, Dict, Optional
from dataclasses import dataclass, field
from enum import Enum
from datetime import datetime
import uuid
from .db import db


class TicketStatus(Enum):
    PENDING = "pending"       # 待处理 -> 对应数据库 OPEN
    PROCESSING = "processing"  # 处理中 -> 对应数据库 IN_PROGRESS
    PENDING_REVIEW = "pending_review"  # 待审核
    COMPLETED = "completed"   # 已完成 -> 对应数据库 RESOLVED
    CLOSED = "closed"         # 已关闭 -> 对应数据库 CLOSED
    REJECTED = "rejected"     # 已拒绝
    REOPENED = "reopened"     # 已重开


class TicketType(Enum):
    RETURN_REFUND = "return_refund"
    EXCHANGE = "exchange"
    ONLY_REFUND = "only_refund"
    COMPLAINT = "complaint"
    COMPENSATION = "compensation"
    AFTER_SALE = "after_sale"
    OTHER = "other"
    # 兼容数据库中的其他类型
    QUALITY_ISSUE = "quality_issue"
    LOGISTICS_ISSUE = "logistics_issue"
    SERVICE_ISSUE = "service_issue"


class TicketPriority(Enum):
    LOW = "low"       # 对应数据库 LOW
    MEDIUM = "medium" # 对应数据库 NORMAL
    HIGH = "high"     # 对应数据库 HIGH
    CRITICAL = "critical"  # 对应数据库 URGENT


# 状态转换规则（数据库状态映射）
VALID_TRANSITIONS = {
    "pending": ["processing", "closed"],
    "processing": ["pending_review", "pending", "rejected"],
    "pending_review": ["completed", "processing", "rejected"],
    "completed": ["closed", "reopened"],
    "closed": ["reopened"],
    "rejected": ["reopened", "closed"],
    "reopened": ["pending", "processing"],
}

# 数据库状态映射
DB_STATUS_MAP = {
    "pending": "OPEN",
    "processing": "IN_PROGRESS",
    "pending_review": "PENDING_REVIEW",
    "completed": "RESOLVED",
    "closed": "CLOSED",
    "rejected": "REJECTED",
    "reopened": "REOPENED"
}

DB_STATUS_REVERSE_MAP = {v: k for k, v in DB_STATUS_MAP.items()}

# 数据库优先级映射
DB_PRIORITY_MAP = {
    "low": "LOW",
    "medium": "NORMAL",
    "high": "HIGH",
    "critical": "URGENT"
}

DB_PRIORITY_REVERSE_MAP = {v: k for k, v in DB_PRIORITY_MAP.items()}


@dataclass
class Ticket:
    ticket_id: str
    merchant_id: int
    user_id: int
    user_name: str
    user_phone: str
    order_id: Optional[str] = None
    ticket_type: TicketType = TicketType.OTHER
    title: str = ""
    description: str = ""
    status: TicketStatus = TicketStatus.PENDING
    priority: TicketPriority = TicketPriority.MEDIUM
    assignee_id: Optional[int] = None
    assignee_name: Optional[str] = None
    conversation_id: Optional[str] = None
    intent: Optional[str] = None
    sentiment: Optional[str] = None
    risk_level: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: datetime = field(default_factory=datetime.now)
    resolved_at: Optional[datetime] = None
    close_reason: Optional[str] = None
    attachments: List[str] = field(default_factory=list)
    logs: List[Dict] = field(default_factory=list)

    def to_dict(self) -> Dict:
        return {
            "ticket_id": self.ticket_id,
            "merchant_id": self.merchant_id,
            "user_id": self.user_id,
            "user_name": self.user_name,
            "user_phone": self.user_phone,
            "order_id": self.order_id,
            "ticket_type": self.ticket_type.value,
            "title": self.title,
            "description": self.description,
            "status": self.status.value,
            "priority": self.priority.value,
            "assignee_id": self.assignee_id,
            "assignee_name": self.assignee_name,
            "conversation_id": self.conversation_id,
            "intent": self.intent,
            "sentiment": self.sentiment,
            "risk_level": self.risk_level,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
            "resolved_at": self.resolved_at.isoformat() if self.resolved_at else None,
            "close_reason": self.close_reason,
            "attachments": self.attachments,
            "logs": self.logs
        }


class TicketManager:
    """工单管理器 - 使用MySQL数据库"""

    def generate_ticket_id(self, merchant_id: int) -> str:
        return f"TKT-{merchant_id}-{datetime.now().strftime('%Y%m%d%H%M%S')}-{str(uuid.uuid4())[:8]}"

    def _generate_title(self, ticket_type: str, description: str) -> str:
        type_map = {
            "return_refund": "退货退款",
            "exchange": "换货",
            "only_refund": "仅退款",
            "complaint": "投诉",
            "compensation": "赔偿",
            "after_sale": "售后咨询",
            "other": "其他"
        }
        title = type_map.get(ticket_type, "其他")
        if description:
            title += f" - {description[:20]}"
        return title

    async def create_ticket(
        self,
        merchant_id: int,
        user_id: int,
        user_name: str,
        user_phone: str,
        ticket_type: str = "other",
        title: str = "",
        description: str = "",
        order_id: Optional[str] = None,
        conversation_id: Optional[str] = None,
        intent: Optional[str] = None,
        sentiment: Optional[str] = None,
        risk_level: Optional[str] = None,
        priority: str = "medium"
    ) -> Ticket:
        """创建工单并保存到数据库"""
        ticket_id = self.generate_ticket_id(merchant_id)
        ticket_title = title or self._generate_title(ticket_type, description)
        db_status = DB_STATUS_MAP.get("pending", "OPEN")
        db_priority = DB_PRIORITY_MAP.get(priority, "NORMAL")

        # 插入工单到数据库
        sql = """
        INSERT INTO ticket (
            ticket_no, user_id, merchant_id, ticket_type, title, description,
            status, priority, ai_category, created_at, updated_at
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        params = (
            ticket_id, user_id, merchant_id, ticket_type, ticket_title, description,
            db_status, db_priority, intent or None, datetime.now(), datetime.now()
        )

        await db.execute(sql, params)

        # 创建工单记录
        ticket = Ticket(
            ticket_id=ticket_id,
            merchant_id=merchant_id,
            user_id=user_id,
            user_name=user_name,
            user_phone=user_phone,
            order_id=order_id,
            ticket_type=TicketType(ticket_type),
            title=ticket_title,
            description=description,
            priority=TicketPriority(priority),
            conversation_id=conversation_id,
            intent=intent,
            sentiment=sentiment,
            risk_level=risk_level
        )

        return ticket

    async def get_ticket(self, ticket_id: str) -> Optional[Ticket]:
        """从数据库获取工单"""
        sql = """
        SELECT t.*, u.nickname as user_name, u.phone as user_phone,
               ms.staff_name as assignee_name
        FROM ticket t
        LEFT JOIN sys_user u ON t.user_id = u.id
        LEFT JOIN merchant_staff ms ON t.assigned_staff_id = ms.id
        WHERE t.ticket_no = %s AND t.deleted = 0
        """
        row = await db.fetch_one(sql, (ticket_id,))
        if not row:
            return None

        return self._row_to_ticket(row)

    async def get_tickets(
        self,
        merchant_id: Optional[int] = None,
        user_id: Optional[int] = None,
        status: Optional[str] = None,
        ticket_type: Optional[str] = None,
        priority: Optional[str] = None,
        limit: int = 20,
        offset: int = 0
    ) -> List[Ticket]:
        """从数据库获取工单列表"""
        sql = """
        SELECT t.*, u.nickname as user_name, u.phone as user_phone,
               ms.staff_name as assignee_name
        FROM ticket t
        LEFT JOIN sys_user u ON t.user_id = u.id
        LEFT JOIN merchant_staff ms ON t.assigned_staff_id = ms.id
        WHERE t.deleted = 0
        """
        params = []

        if merchant_id:
            sql += " AND t.merchant_id = %s"
            params.append(merchant_id)
        if user_id:
            sql += " AND t.user_id = %s"
            params.append(user_id)
        if status:
            db_status = DB_STATUS_MAP.get(status, status.upper())
            sql += " AND t.status = %s"
            params.append(db_status)
        if ticket_type:
            sql += " AND t.ticket_type = %s"
            params.append(ticket_type)
        if priority:
            db_priority = DB_PRIORITY_MAP.get(priority, priority.upper())
            sql += " AND t.priority = %s"
            params.append(db_priority)

        sql += " ORDER BY t.created_at DESC LIMIT %s OFFSET %s"
        params.extend([limit, offset])

        rows = await db.fetch_all(sql, tuple(params))
        return [self._row_to_ticket(row) for row in rows]

    def _row_to_ticket(self, row: Dict) -> Ticket:
        """将数据库行转换为Ticket对象"""
        db_status = row.get("status", "OPEN")
        status = DB_STATUS_REVERSE_MAP.get(db_status, db_status.lower())
        
        db_priority = row.get("priority", "NORMAL")
        priority = DB_PRIORITY_REVERSE_MAP.get(db_priority, db_priority.lower())

        # 处理ticket_type，容错处理未定义的类型
        db_ticket_type = row.get("ticket_type", "other")
        try:
            ticket_type = TicketType(db_ticket_type)
        except ValueError:
            ticket_type = TicketType.OTHER

        return Ticket(
            ticket_id=row.get("ticket_no", ""),
            merchant_id=row.get("merchant_id", 0),
            user_id=row.get("user_id", 0),
            user_name=row.get("user_name", ""),
            user_phone=row.get("user_phone", ""),
            order_id=None,  # 需要关联查询external_order
            ticket_type=ticket_type,
            title=row.get("title", ""),
            description=row.get("description", ""),
            status=TicketStatus(status),
            priority=TicketPriority(priority),
            assignee_id=row.get("assigned_staff_id"),
            assignee_name=row.get("assignee_name"),
            conversation_id=row.get("conversation_id"),
            intent=row.get("ai_category"),
            sentiment=None,
            risk_level=None,
            created_at=row.get("created_at"),
            updated_at=row.get("updated_at"),
            resolved_at=row.get("closed_at"),
            close_reason=None,
            attachments=[],
            logs=[]
        )

    async def assign_ticket(self, ticket_id: str, assignee_id: int, assignee_name: str) -> bool:
        """分配工单给客服"""
        sql = """
        UPDATE ticket SET assigned_staff_id = %s, status = %s, updated_at = %s
        WHERE ticket_no = %s AND deleted = 0
        """
        params = (assignee_id, "IN_PROGRESS", datetime.now(), ticket_id)
        result = await db.execute(sql, params)
        return result > 0

    async def update_status(self, ticket_id: str, new_status: str, operator: str = "system", detail: str = "") -> bool:
        """更新工单状态"""
        # 先获取当前状态
        ticket = await self.get_ticket(ticket_id)
        if not ticket:
            return False

        current_status = ticket.status.value
        if new_status not in VALID_TRANSITIONS.get(current_status, []):
            return False

        db_status = DB_STATUS_MAP.get(new_status, new_status.upper())
        resolved_at = None
        
        if new_status in ["completed", "closed"]:
            resolved_at = datetime.now()

        sql = """
        UPDATE ticket SET status = %s, updated_at = %s, closed_at = %s
        WHERE ticket_no = %s AND deleted = 0
        """
        params = (db_status, datetime.now(), resolved_at, ticket_id)
        result = await db.execute(sql, params)
        return result > 0

    async def close_ticket(self, ticket_id: str, close_reason: str = "", operator: str = "system") -> bool:
        """关闭工单"""
        sql = """
        UPDATE ticket SET status = %s, updated_at = %s, closed_at = %s
        WHERE ticket_no = %s AND deleted = 0
        """
        params = ("CLOSED", datetime.now(), datetime.now(), ticket_id)
        result = await db.execute(sql, params)
        return result > 0

    async def add_note(self, ticket_id: str, note: str, operator: str = "system") -> bool:
        """添加工单备注"""
        # 需要先获取工单的数据库ID
        ticket = await self.get_ticket(ticket_id)
        if not ticket:
            return False

        # 查询数据库ID
        sql_id = "SELECT id FROM ticket WHERE ticket_no = %s"
        row = await db.fetch_one(sql_id, (ticket_id,))
        if not row:
            return False

        db_ticket_id = row.get("id")
        
        # 添加处理记录
        sql = """
        INSERT INTO ticket_record (ticket_id, operator_id, action_type, from_status, to_status, content, created_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        """
        # operator_id暂时使用1（系统用户）
        params = (db_ticket_id, 1, "note_added", None, None, note, datetime.now())
        result = await db.execute(sql, params)
        return result > 0

    async def delete_ticket(self, ticket_id: str) -> bool:
        """删除工单（软删除）"""
        sql = "UPDATE ticket SET deleted = 1, updated_at = %s WHERE ticket_no = %s"
        params = (datetime.now(), ticket_id)
        result = await db.execute(sql, params)
        return result > 0

    async def get_ticket_count(
        self, 
        merchant_id: Optional[int] = None, 
        user_id: Optional[int] = None,
        status: Optional[str] = None
    ) -> int:
        """获取工单数量"""
        sql = "SELECT COUNT(*) as count FROM ticket WHERE deleted = 0"
        params = []

        if merchant_id:
            sql += " AND merchant_id = %s"
            params.append(merchant_id)
        if user_id:
            sql += " AND user_id = %s"
            params.append(user_id)
        if status:
            db_status = DB_STATUS_MAP.get(status, status.upper())
            sql += " AND status = %s"
            params.append(db_status)

        row = await db.fetch_one(sql, tuple(params))
        return row.get("count", 0) if row else 0


# 全局工单管理器实例
ticket_manager = TicketManager()