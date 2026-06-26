from app.api.schemas import ContextReplyRequest, ReplyResponse
from app.intent.classifier import classify_intent


def build_reply(request: ContextReplyRequest) -> ReplyResponse:
    intent = classify_intent(request.text)
    if intent.intent == "RETURN_REFUND":
        reply = "您好，已收到您的退货退款诉求。请先上传商品照片、包装照片和问题说明，我们会尽快为您审核。"
        suggestions = ["核对订单状态", "引导上传凭证", "说明退款时效"]
    elif intent.intent == "LOGISTICS":
        reply = "您好，我们会帮您核实最新物流轨迹。如果物流长时间未更新，会协助联系快递处理。"
        suggestions = ["查询物流快照", "核实签收状态", "必要时转人工"]
    elif intent.intent == "COMPLAINT":
        reply = "非常抱歉给您带来不好的体验。我们会优先记录并升级处理，请您补充问题细节。"
        suggestions = ["优先安抚", "创建高优先级工单", "跟进处理结果"]
    else:
        reply = "您好，您的问题已经收到，我们会根据订单和售后规则尽快协助处理。"
        suggestions = ["确认订单", "识别诉求", "必要时转人工"]
    return ReplyResponse(
        reply=reply,
        intent=intent.intent,
        confidence=intent.confidence,
        suggestions=suggestions,
    )
