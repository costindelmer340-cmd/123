from typing import Optional

class RAGGenerator:
    def __init__(self):
        self.faq_knowledge = {
            "refund": "退款政策：订单完成后7天内可申请退款，需保持商品完好，退款将在3-5个工作日内原路返回。",
            "exchange": "换货政策：支持7天无理由换货，运费由商家承担，建议先联系客服确认。",
            "shipping": "物流查询：您可以在订单详情页查看物流信息，通常下单后48小时内发货。",
            "complaint": "投诉建议：如对服务不满意，请通过售后申请或联系客服反馈，我们会尽快处理。",
            "return": "退货流程：申请售后→等待审核→寄回商品→确认收货→退款完成。"
        }
    
    def generate_response(self, intent: str, query: str) -> str:
        responses = {
            "REFUND": self.faq_knowledge["refund"],
            "EXCHANGE": self.faq_knowledge["exchange"],
            "COMPLAINT": self.faq_knowledge["complaint"],
            "INQUIRY": self.faq_knowledge["shipping"],
            "CANCEL": "如需取消订单或售后申请，请在订单详情页操作，或联系客服协助处理。",
            "HELP": "请问需要什么帮助？我可以帮您查询订单、申请售后、了解退款政策等。",
            "THANKS": "不客气，很高兴能帮到您！如有其他问题随时联系我们。"
        }
        
        return responses.get(intent, "抱歉，我不太理解您的问题，请联系客服获取帮助。")

rag_generator = RAGGenerator()