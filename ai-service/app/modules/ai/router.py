from fastapi import APIRouter, Query, Depends, Body
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.modules.ai.intent_recognizer import intent_recognizer
from app.modules.ai.sentiment_analyzer import sentiment_analyzer
from app.modules.ai.rag_generator import rag_generator
from modules.llm_client import llm_client
from modules.faq_knowledge_base import faq_knowledge_base
from app.common.response import APIResponse
from typing import Optional
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/ai", tags=["AI服务"])

class AiTextRequest(BaseModel):
    text: str = Field(..., description="文本内容")
    merchantId: int = Field(None, description="商家ID")
    businessType: str = Field(None, description="业务类型")
    businessId: int = Field(None, description="业务ID")

class AiReplyRequest(BaseModel):
    text: str = Field(..., description="文本内容")
    merchantId: Optional[int] = Field(None, description="商家ID")
    businessType: Optional[str] = Field(None, description="业务类型")
    businessId: Optional[int] = Field(None, description="业务ID")
    orderStatus: Optional[str] = Field(None, description="订单状态")
    afterSaleStatus: Optional[str] = Field(None, description="售后状态")
    userTone: Optional[str] = Field(None, description="用户语气")

@router.post("/chat", response_model=APIResponse)
def chat(
    message: str = Query(...),
    conversation_id: str = Query(None),
    context: str = Query(None),
    db: Session = Depends(get_db)
):
    intent_result = intent_recognizer.recognize(message)
    sentiment_result = sentiment_analyzer.analyze(message)
    
    search_results = faq_knowledge_base.search(message, intent_result.intent)
    knowledge_items = [
        {"title": r.faq.question, "content": r.faq.answer, "match_score": r.match_score}
        for r in search_results
    ]
    
    llm_response = None
    if llm_client.is_configured():
        llm_response = llm_client.generate_response(
            query=message,
            intent=intent_result.intent,
            sentiment=sentiment_result.sentiment,
            knowledge_items=knowledge_items
        )
    
    if llm_response:
        response_text = llm_response
    else:
        response_text = rag_generator.generate_response(intent_result.intent, message)
    
    return APIResponse.success({
        "conversation_id": conversation_id,
        "response": response_text,
        "intent": {
            "intent": intent_result.intent,
            "confidence": intent_result.confidence,
            "entities": intent_result.entities
        },
        "sentiment": {
            "sentiment": sentiment_result.sentiment,
            "score": sentiment_result.score
        },
        "context": context,
        "message_id": f"msg_{hash(message)}",
        "llm_used": llm_client.is_configured()
    })

@router.post("/intent", response_model=APIResponse)
def analyze_intent(query: str = Query(...)):
    result = intent_recognizer.recognize(query)
    return APIResponse.success({
        "intent": result.intent,
        "confidence": result.confidence,
        "entities": result.entities
    })

@router.post("/intent", response_model=APIResponse)
def api_analyze_intent(request: AiTextRequest = Body(...)):
    result = intent_recognizer.recognize(request.text)
    return APIResponse.success({
        "intent": result.intent,
        "confidence": result.confidence,
        "entities": result.entities
    })

@router.post("/sentiment", response_model=APIResponse)
def analyze_sentiment(text: str = Query(...)):
    result = sentiment_analyzer.analyze(text)
    return APIResponse.success({
        "sentiment": result.sentiment,
        "score": result.score
    })

@router.post("/sentiment", response_model=APIResponse)
def api_analyze_sentiment(request: AiTextRequest = Body(...)):
    result = sentiment_analyzer.analyze(request.text)
    return APIResponse.success({
        "sentiment": result.sentiment,
        "score": result.score
    })

@router.post("/topic", response_model=APIResponse)
def extract_topic(text: str = Query(...)):
    topics = []
    keywords = ["订单", "退款", "退货", "换货", "物流", "客服", "问题", "投诉"]
    for keyword in keywords:
        if keyword in text:
            topics.append(keyword)
    
    return APIResponse.success({
        "topics": topics if topics else ["其他"],
        "topic_keywords": topics
    })

@router.post("/topic", response_model=APIResponse)
def api_extract_topic(request: AiTextRequest = Body(...)):
    topics = []
    keywords = ["订单", "退款", "退货", "换货", "物流", "客服", "问题", "投诉"]
    for keyword in keywords:
        if keyword in request.text:
            topics.append(keyword)
    
    return APIResponse.success({
        "topics": topics if topics else ["其他"],
        "topic_keywords": topics
    })

@router.post("/ticket/classify", response_model=APIResponse)
def api_classify_ticket(request: AiTextRequest = Body(...)):
    intent_keywords = {
        "REFUND": ["退款", "退钱", "返还", "退费"],
        "RETURN": ["退货", "退", "寄回", "返还商品"],
        "EXCHANGE": ["换货", "换", "更换"],
        "COMPLAINT": ["投诉", "举报", "不满", "问题"]
    }
    
    intent = "OTHER"
    confidence = 0.0
    
    text = request.text
    for key, keywords in intent_keywords.items():
        for keyword in keywords:
            if keyword in text:
                intent = key
                confidence = min(0.8 + len(keyword)/10, 1.0)
                break
        if intent != "OTHER":
            break
    
    return APIResponse.success({
        "ticketType": intent,
        "confidence": confidence
    })

@router.post("/reply", response_model=APIResponse)
def api_generate_reply(request: AiReplyRequest = Body(...)):
    intent_result = intent_recognizer.recognize(request.text)
    sentiment_result = sentiment_analyzer.analyze(request.text)
    
    search_results = faq_knowledge_base.search(request.text, intent_result.intent)
    knowledge_items = [
        {"title": r.faq.question, "content": r.faq.answer, "match_score": r.match_score}
        for r in search_results
    ]
    
    llm_response = None
    if llm_client.is_configured():
        llm_response = llm_client.generate_response(
            query=request.text,
            intent=intent_result.intent,
            sentiment=sentiment_result.sentiment,
            knowledge_items=knowledge_items
        )
    
    if llm_response:
        response_text = llm_response
    else:
        response_text = rag_generator.generate_response(intent_result.intent, request.text)
    
    return APIResponse.success({
        "reply": response_text,
        "intent": intent_result.intent,
        "confidence": intent_result.confidence,
        "sentiment": sentiment_result.sentiment,
        "sentimentScore": sentiment_result.score,
        "isEscalate": False
    })

@router.post("/rag", response_model=APIResponse)
def rag_query(
    question: str = Query(...),
    intent: str = Query(None),
    db: Session = Depends(get_db)
):
    if not intent:
        intent_result = intent_recognizer.recognize(question)
        intent = intent_result.intent
    
    search_results = faq_knowledge_base.search(question, intent)
    knowledge_items = [
        {"title": r.faq.question, "content": r.faq.answer}
        for r in search_results
    ]
    
    llm_response = None
    if llm_client.is_configured():
        llm_response = llm_client.generate_response(
            query=question,
            intent=intent,
            sentiment="NEUTRAL",
            knowledge_items=knowledge_items
        )
    
    if llm_response:
        answer = llm_response
    else:
        answer = rag_generator.generate_response(intent, question)
    
    return APIResponse.success({
        "answer": answer,
        "source": "llm+faq" if llm_client.is_configured() else "faq_knowledge",
        "references": [item.get("title", "") for item in knowledge_items],
        "llm_used": llm_client.is_configured()
    })

@router.get("/config", response_model=APIResponse)
def get_ai_config():
    from app.config import settings
    return APIResponse.success({
        "model_provider": settings.MODEL_PROVIDER,
        "llm_configured": llm_client.is_configured(),
        "temperature": settings.MODEL_TEMPERATURE,
        "max_tokens": settings.MODEL_MAX_TOKENS
    })

@router.post("/llm/test", response_model=APIResponse)
def test_llm_connection(prompt: str = Query("你好")):
    if not llm_client.is_configured():
        return APIResponse.error(400, "LLM API Key未配置")
    
    response = llm_client.chat_completion([{"role": "user", "content": prompt}])
    
    if response:
        return APIResponse.success({
            "success": True,
            "response": response,
            "model": llm_client.model_name
        })
    else:
        return APIResponse.error(500, "LLM调用失败")

@router.post("/faq/search", response_model=APIResponse)
def search_faq(
    query: str = Query(...),
    intent: str = Query(None),
    limit: int = Query(5)
):
    results = faq_knowledge_base.search(query, intent, limit)
    return APIResponse.success({
        "results": results,
        "total": len(results)
    })

@router.post("/faq/add", response_model=APIResponse)
def add_faq(
    title: str = Query(...),
    content: str = Query(...),
    intent: str = Query("OTHER"),
    keywords: str = Query(None)
):
    keyword_list = keywords.split(",") if keywords else []
    faq_knowledge_base.add_knowledge(title, content, intent, keyword_list)
    return APIResponse.success({"message": "FAQ知识添加成功"})