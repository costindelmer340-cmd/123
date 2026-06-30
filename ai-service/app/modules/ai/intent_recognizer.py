from typing import Optional, Dict

class IntentResult:
    def __init__(self, intent: str, confidence: float, entities: Optional[Dict] = None):
        self.intent = intent
        self.confidence = confidence
        self.entities = entities or {}

class IntentRecognizer:
    def __init__(self):
        self.intent_patterns = {
            "REFUND": ["退款", "退货", "退钱", "返还", "退款申请", "退货退款"],
            "EXCHANGE": ["换货", "换", "更换", "尺码", "颜色", "型号"],
            "COMPLAINT": ["投诉", "差评", "不满意", "问题", "质量", "服务"],
            "INQUIRY": ["查询", "查", "订单", "物流", "进度", "状态"],
            "CANCEL": ["取消", "撤销", "放弃"],
            "HELP": ["帮助", "客服", "指导", "怎么", "如何", "教程"],
            "THANKS": ["谢谢", "感谢", "好的", "知道了"],
            "OTHER": []
        }
    
    def recognize(self, text: str) -> IntentResult:
        text = text.lower()
        max_score = 0
        matched_intent = "OTHER"
        
        for intent, patterns in self.intent_patterns.items():
            if intent == "OTHER":
                continue
            score = sum(1 for pattern in patterns if pattern in text)
            if score > max_score:
                max_score = score
                matched_intent = intent
        
        confidence = min(max_score / 3, 1.0) if max_score > 0 else 0.3
        
        entities = self._extract_entities(text)
        
        return IntentResult(intent=matched_intent, confidence=confidence, entities=entities)
    
    def _extract_entities(self, text: str) -> Dict:
        entities = {}
        
        import re
        order_match = re.search(r'(订单号|订单)([a-zA-Z0-9]+)', text)
        if order_match:
            entities["order_no"] = order_match.group(2)
        
        amount_match = re.search(r'(\d+(?:\.\d+)?)(元|块|钱)', text)
        if amount_match:
            entities["amount"] = float(amount_match.group(1))
        
        return entities

intent_recognizer = IntentRecognizer()