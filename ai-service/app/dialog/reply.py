from app.api.schemas import ContextReplyRequest, ReplyResponse
from app.intent.classifier import classify_intent
from app.llm_client import llm_client
from app.db.order_query import extract_order_no, build_context, search_order_by_product
from app.dialog.context_manager import context_manager
from app.rag.service import rag_service
import difflib


def get_rule_based_reply(intent: str, text: str, order_context: str = "") -> str:
    intent_rules = {
        "RETURN_REFUND": "您好，关于退货退款问题，需要您提供订单号以便我帮您查询订单状态。请确认商品是否已签收？如有质量问题，请提供相关凭证照片。",
        "EXCHANGE": "您好，换货服务需要商品未使用且包装完好。请提供订单号，我将为您办理换货申请。",
        "REPAIR": "您好，关于维修问题，请提供订单号和问题描述，如有照片请一并提供，以便我为您核实处理。",
        "LOGISTICS": "您好，为了帮您查询物流信息，请提供订单号。我将立即为您查询最新的物流状态。",
        "COMPLAINT": "非常抱歉给您带来不好的体验！请详细描述您的问题，我会尽力为您解决。如需人工协助，我可以帮您转接。",
    }
    
    if order_context:
        if intent in ["RETURN_REFUND", "EXCHANGE", "REPAIR", "LOGISTICS"]:
            return f"您好，根据您提供的订单信息，我已经了解了您的订单情况。\n\n{order_context}\n\n请问您具体想咨询哪方面的问题呢？"
        return f"您好，我已经查询到您的订单信息：\n\n{order_context}\n\n请问有什么可以帮您的？"
    
    if intent in intent_rules:
        return intent_rules[intent]
    
    if any(keyword in text for keyword in ["退款", "退钱", "返款"]):
        return "您好，如需申请退款，请提供订单号，我将为您查询订单状态并协助办理退款手续。"
    if any(keyword in text for keyword in ["退货", "退"]):
        return "您好，如需退货，请确认商品是否符合退货条件。请提供订单号，我将协助您办理退货申请。"
    if any(keyword in text for keyword in ["物流", "快递", "发货", "运输"]):
        return "您好，为了帮您查询物流信息，请提供订单号，我将立即为您追踪包裹位置。"
    if any(keyword in text for keyword in ["客服", "人工"]):
        return "好的，我将为您转接人工客服，请稍等片刻。"
    if any(keyword in text for keyword in ["投诉", "差评", "不满意"]):
        return "非常抱歉给您带来不愉快的购物体验！请详细说明您的问题，我会尽力为您解决。"
    
    return "您好，请问有什么可以帮您的？如需查询订单或售后相关问题，请提供订单号，我将为您处理。"


def _is_duplicate_question(session_id: str, text: str) -> tuple[bool, str | None]:
    if not session_id:
        return False, None
    history = context_manager.get_history(session_id)
    if not history:
        return False, None
    
    user_messages = [m["content"] for m in history if m["sender"] == "user"]
    ai_messages = [m["content"] for m in history if m["sender"] == "ai"]
    
    if len(user_messages) < 1:
        return False, None
    
    last_user_msg = user_messages[-1]
    
    text_clean = text.strip()
    last_clean = last_user_msg.strip()
    
    if text_clean == last_clean:
        if ai_messages:
            return True, ai_messages[-1]
        return True, None
    
    similarity = difflib.SequenceMatcher(None, text_clean, last_clean).ratio()
    
    if similarity > 0.85:
        if ai_messages:
            return True, ai_messages[-1]
        return True, None
    
    return False, None


def build_reply(request: ContextReplyRequest) -> ReplyResponse:
    intent = classify_intent(request.text)
    
    if request.session_id:
        is_duplicate, previous_reply = _is_duplicate_question(request.session_id, request.text)
        if is_duplicate:
            if previous_reply:
                reply = f"刚才已经回答过这个问题了：{previous_reply[:60]}..."
                if request.session_id and reply:
                    context_manager.add_message(request.session_id, "ai", reply)
                return ReplyResponse(
                    reply=reply,
                    intent=intent.intent,
                    confidence=intent.confidence,
                    suggestions=["继续提问", "转人工客服"],
                )
    
    order_no = extract_order_no(request.text)
    order_context = ""
    
    if request.session_id:
        context_order_no = context_manager.get_order_no_from_context(request.session_id)
        if not order_no and context_order_no:
            order_no = context_order_no
    
    if request.session_id and not order_no:
        candidate_orders = context_manager.get_candidate_orders(request.session_id)
        if candidate_orders:
            num_match = __import__('re').search(r'^(\d+)$', request.text.strip())
            if num_match:
                index = int(num_match.group(1)) - 1
                if 0 <= index < len(candidate_orders):
                    order_no = candidate_orders[index]["order_no"]
                    order_context = build_context(order_no)
    
    if order_no:
        order_context = build_context(order_no)
    
    if not order_no and not order_context:
        search_results = search_order_by_product(request.text)
        if len(search_results) == 1:
            order_no = search_results[0]["order_no"]
            order_context = build_context(order_no)
        elif len(search_results) > 1:
            product_list = "\n".join([f"{i+1}. {item['product_name']} (订单号: {item['order_no']})" for i, item in enumerate(search_results[:5])])
            reply = f"您好，我查询到多个相关订单，请确认您想咨询的是哪个：\n\n{product_list}\n\n请回复序号或订单号。"
            if request.session_id:
                context_manager.add_message(request.session_id, "user", request.text)
                context_manager.add_message(request.session_id, "ai", reply)
                context_manager.set_candidate_orders(request.session_id, search_results[:5])
            return ReplyResponse(
                reply=reply,
                intent=intent.intent,
                confidence=intent.confidence,
                suggestions=["提供订单号", "选择序号", "转人工客服"],
            )
    
    if request.session_id:
        context_manager.add_message(request.session_id, "user", request.text)
        if order_no and order_context:
            context_manager.update_order_context(request.session_id, order_no, order_context)
    
    context_summary = ""
    if request.session_id:
        context_summary = context_manager.get_context_summary(request.session_id)
    
    knowledge_context = ""
    if intent.intent not in ["GREETING", "UNKNOWN"]:
        try:
            knowledge_context = rag_service.get_knowledge_context(
                query=request.text,
                top_k=3,
                merchant_id=request.merchant_id
            )
        except Exception:
            knowledge_context = ""
    
    if llm_client.is_configured():
        reply = llm_client.generate_after_sale_reply(
            text=request.text,
            intent=intent.intent,
            category=intent.category,
            order_status=request.order_status,
            after_sale_status=request.after_sale_status,
            user_tone=request.user_tone,
            order_context=order_context,
            context_summary=context_summary,
            knowledge_context=knowledge_context,
        )
    else:
        reply = get_rule_based_reply(intent.intent, request.text, order_context)
    
    if request.session_id and reply:
        context_manager.add_message(request.session_id, "ai", reply)
    
    suggestions = []
    if intent.intent in ["RETURN_REFUND", "REPAIR", "LOGISTICS"] and not order_no:
        suggestions.append("补充订单信息")
    if intent.intent in ["COMPLAINT", "REPAIR"]:
        suggestions.append("补充问题凭证")
    suggestions.append("转人工客服")
    
    if not reply:
        reply = "AI 服务暂不可用，建议为您转接人工客服继续处理。"
    
    return ReplyResponse(
        reply=reply,
        intent=intent.intent,
        confidence=intent.confidence,
        suggestions=suggestions,
    )
