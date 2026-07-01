import redis
import json
import time
from app.config import settings


class ContextManager:
    def __init__(self):
        try:
            self.redis_client = redis.Redis(
                host="localhost",
                port=6379,
                decode_responses=True
            )
        except Exception:
            self.redis_client = None
        self.session_timeout = 3600
        self.short_term_limit = 10
        self.long_term_limit = 50
        self.summary_threshold = 8

    def get_session(self, session_id: str) -> dict:
        if not self.redis_client:
            return {"order_no": None, "order_context": "", "history": []}
        try:
            data = self.redis_client.get(f"chat_session:{session_id}")
            if data:
                session_data = json.loads(data)
                if time.time() - session_data.get("last_updated", 0) < self.session_timeout:
                    return session_data
                else:
                    self.redis_client.delete(f"chat_session:{session_id}")
        except Exception:
            pass
        return {"order_no": None, "order_context": "", "history": []}

    def save_session(self, session_id: str, session_data: dict):
        if not self.redis_client:
            return
        session_data["last_updated"] = time.time()
        self.redis_client.setex(f"chat_session:{session_id}", self.session_timeout, json.dumps(session_data, ensure_ascii=False))

    def update_order_context(self, session_id: str, order_no: str, order_context: str):
        session_data = self.get_session(session_id)
        session_data["order_no"] = order_no
        session_data["order_context"] = order_context
        session_data.pop("candidate_orders", None)
        self.save_session(session_id, session_data)

    def set_candidate_orders(self, session_id: str, orders: list[dict]):
        session_data = self.get_session(session_id)
        session_data["candidate_orders"] = orders
        self.save_session(session_id, session_data)

    def get_candidate_orders(self, session_id: str) -> list[dict]:
        session_data = self.get_session(session_id)
        return session_data.get("candidate_orders", [])

    def get_order_no_from_context(self, session_id: str) -> str | None:
        session_data = self.get_session(session_id)
        return session_data.get("order_no")

    def get_order_context(self, session_id: str) -> str:
        session_data = self.get_session(session_id)
        return session_data.get("order_context", "")

    def add_message(self, session_id: str, sender: str, content: str):
        if not self.redis_client:
            return
        session_data = self.get_session(session_id)
        history = session_data.get("history", [])
        history.append({
            "sender": sender,
            "content": content,
            "timestamp": time.time()
        })
        
        long_term_memory = session_data.get("long_term_memory", [])
        
        if sender == "user":
            self._update_long_term_memory(long_term_memory, content)
        
        if len(long_term_memory) > self.long_term_limit:
            long_term_memory = long_term_memory[-self.long_term_limit:]
        
        if len(history) > self.short_term_limit:
            history = history[-self.short_term_limit:]
        
        session_data["history"] = history
        session_data["long_term_memory"] = long_term_memory
        
        if len(history) >= self.summary_threshold and len(history) % self.summary_threshold == 0:
            session_data["last_summary"] = self._generate_basic_summary(session_data)
        
        self.save_session(session_id, session_data)
    
    def _update_long_term_memory(self, memory: list, content: str):
        order_patterns = [
            r'订单[号:：]?\s*([A-Za-z0-9]+)',
            r'订单\s*(\d+)',
            r'(\d{10,})',
        ]
        import re
        for pattern in order_patterns:
            match = re.search(pattern, content)
            if match:
                order_no = match.group(1)
                existing = next((m for m in memory if m["type"] == "order_no"), None)
                if existing:
                    existing["value"] = order_no
                else:
                    memory.append({
                        "type": "order_no",
                        "value": order_no,
                        "updated_at": time.time()
                    })
                break
        
        refund_keywords = ["退款", "退货", "退钱", "返款", "售后"]
        if any(kw in content for kw in refund_keywords):
            existing = next((m for m in memory if m["type"] == "intent"), None)
            if existing:
                existing["value"] = "售后/退款"
            else:
                memory.append({
                    "type": "intent",
                    "value": "售后/退款",
                    "updated_at": time.time()
                })
    
    def _generate_basic_summary(self, session_data: dict) -> str:
        history = session_data.get("history", [])
        order_no = session_data.get("order_no")
        
        summary_parts = []
        
        if order_no:
            summary_parts.append(f"订单号：{order_no}")
        
        if history:
            user_messages = [m["content"] for m in history if m["sender"] == "user"]
            if user_messages:
                summary_parts.append(f"用户咨询：{'; '.join(user_messages[-3:])}")
            
            ai_messages = [m["content"] for m in history if m["sender"] == "ai"]
            if ai_messages:
                summary_parts.append(f"已回复要点：{ai_messages[-1][:50]}...")
        
        return "\n".join(summary_parts)

    def get_history(self, session_id: str) -> list[dict]:
        session_data = self.get_session(session_id)
        return session_data.get("history", [])

    def get_context_summary(self, session_id: str) -> str:
        session_data = self.get_session(session_id)
        history = session_data.get("history", [])
        order_no = session_data.get("order_no")
        order_context = session_data.get("order_context", "")
        long_term_memory = session_data.get("long_term_memory", [])
        last_summary = session_data.get("last_summary", "")
        
        if len(history) == 0:
            return ""
        
        summary_parts = []
        
        if last_summary:
            summary_parts.append(f"【对话摘要】\n{last_summary}")
        
        if order_no:
            summary_parts.append(f"【关联订单】\n订单号：{order_no}")
        
        if order_context:
            order_info = order_context[:150] + "..." if len(order_context) > 150 else order_context
            summary_parts.append(f"【订单详情】\n{order_info}")
        
        if long_term_memory:
            memory_items = []
            for item in long_term_memory:
                if item["type"] == "order_no":
                    memory_items.append(f"订单号: {item['value']}")
                elif item["type"] == "intent":
                    memory_items.append(f"主要意图: {item['value']}")
            if memory_items:
                summary_parts.append(f"【关键信息】\n{'; '.join(memory_items)}")
        
        recent_history = history[-5:]
        if recent_history:
            history_text = ""
            for msg in recent_history:
                role = "用户" if msg["sender"] == "user" else "AI"
                history_text += f"{role}: {msg['content']}\n"
            summary_parts.append(f"【最近对话】\n{history_text.strip()}")
        
        return "\n\n".join(summary_parts)

    def clear_session(self, session_id: str):
        if not self.redis_client:
            return
        self.redis_client.delete(f"chat_session:{session_id}")


context_manager = ContextManager()