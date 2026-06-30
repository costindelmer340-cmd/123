from fastapi import APIRouter, Query, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.common.response import APIResponse
from modules.rule_engine import rule_engine, RuleResult

router = APIRouter(prefix="/ai/rule", tags=["规则引擎"])

@router.post("/classify", response_model=APIResponse)
def classify_message(text: str = Query(...)):
    intent_keywords = {
        "退款": ["退款", "退钱", "返还", "退费"],
        "退货": ["退货", "退", "寄回", "返还商品"],
        "换货": ["换货", "换", "更换"],
        "物流": ["物流", "快递", "运输", "配送", "发货"],
        "客服": ["客服", "人工", "服务"],
        "投诉": ["投诉", "举报", "不满", "问题"]
    }
    
    intent = "OTHER"
    confidence = 0.0
    
    for key, keywords in intent_keywords.items():
        for keyword in keywords:
            if keyword in text:
                intent = key
                confidence = min(0.8 + len(keyword)/10, 1.0)
                break
        if intent != "OTHER":
            break
    
    return APIResponse.success({
        "intent": intent,
        "confidence": confidence,
        "rules": []
    })

@router.post("/inspect", response_model=APIResponse)
def inspect_rules(
    text: str = Query(...),
    intent: str = Query(None),
    sentiment: str = Query("NEUTRAL"),
    sentiment_score: float = Query(0.0),
    order_amount: float = Query(0),
    issue_count: int = Query(0),
    db: Session = Depends(get_db)
):
    rules = []
    
    rule_data = {
        'text': text,
        'intent': intent or 'OTHER',
        'sentiment': sentiment,
        'sentiment_score': sentiment_score,
        'risk_level': 'HIGH' if sentiment_score < -0.5 else 'MEDIUM' if sentiment_score < 0 else 'LOW',
        'order_amount': order_amount,
        'issue_count': issue_count
    }
    
    escalation_result = rule_engine.execute("escalation", rule_data)
    
    for rule_result in escalation_result.get("rule_results", []):
        rules.append({
            "rule_id": f"R{len(rules)+1:03d}",
            "rule_name": rule_result["rule_name"],
            "rule_description": rule_result["rule_description"],
            "triggered": rule_result["result"] == "escalate",
            "action": "ESCALATE" if rule_result["result"] == "escalate" else "NONE",
            "confidence": 0.9 if rule_result["result"] == "escalate" else 0.0
        })
    
    return APIResponse.success({
        "rules": rules,
        "escalate_required": escalation_result["overall_result"] == "escalate",
        "recommendation": escalation_result["message"]
    })

@router.post("/escalate", response_model=APIResponse)
def escalate(
    text: str = Query(...),
    intent: str = Query(None),
    sentiment: str = Query("NEUTRAL"),
    sentiment_score: float = Query(0.0),
    risk_level: str = Query("LOW"),
    issue_count: int = Query(0),
    order_amount: float = Query(0),
    user_id: int = Query(1),
    order_id: str = Query(None),
    db: Session = Depends(get_db)
):
    rule_data = {
        'text': text,
        'intent': intent or 'OTHER',
        'sentiment': sentiment,
        'sentiment_score': sentiment_score,
        'risk_level': risk_level,
        'issue_count': issue_count,
        'order_amount': order_amount
    }
    
    result = rule_engine.execute("escalation", rule_data)
    should_escalate = result["overall_result"] == "escalate"
    
    ticket_id = None
    priority = "NORMAL"
    
    if should_escalate:
        from datetime import datetime
        import uuid
        ticket_id = f"T{datetime.now().strftime('%Y%m%d%H%M%S')}{uuid.uuid4().hex[:4].upper()}"
        priority = "HIGH" if len(result["failed_rules"]) >= 2 else "MEDIUM"
    
    return APIResponse.success({
        "escalated": should_escalate,
        "ticket_id": ticket_id,
        "reason": result["message"],
        "priority": priority,
        "triggered_rules": result["failed_rules"],
        "user_id": user_id,
        "order_id": order_id
    })

@router.post("/review", response_model=APIResponse)
def review_after_sale(
    after_sale_no: str = Query(...),
    decision: str = Query(...),
    remark: str = Query(None),
    db: Session = Depends(get_db)
):
    return APIResponse.success({
        "after_sale_no": after_sale_no,
        "decision": decision,
        "remark": remark,
        "status": "REVIEWED",
        "reviewed_at": __import__('datetime').datetime.now().isoformat()
    })

@router.post("/execute", response_model=APIResponse)
def execute_rule(
    rule_set: str = Query("after_sale_full"),
    data: str = Query(None),
    db: Session = Depends(get_db)
):
    import json
    params_dict = {}
    if data:
        try:
            params_dict = json.loads(data)
        except:
            pass
    
    result = rule_engine.execute(rule_set, params_dict)
    
    return APIResponse.success({
        "rule_set": rule_set,
        "executed": True,
        "overall_result": result["overall_result"],
        "message": result["message"],
        "rule_results": result["rule_results"]
    })

@router.get("/sets", response_model=APIResponse)
def get_rule_sets(db: Session = Depends(get_db)):
    rule_sets = [
        {
            "set_id": "SET001",
            "name": "售后规则集",
            "rules": ["R001", "R002", "R003"],
            "active": True
        },
        {
            "set_id": "SET002",
            "name": "投诉处理规则",
            "rules": ["R001", "R003"],
            "active": True
        },
        {
            "set_id": "SET003",
            "name": "质检审核规则",
            "rules": ["质检规则", "售后分类规则"],
            "active": True
        }
    ]
    
    return APIResponse.success({
        "rule_sets": rule_sets,
        "total": len(rule_sets)
    })

@router.post("/classify-after-sale", response_model=APIResponse)
def classify_after_sale(
    text: str = Query(...),
    user_id: int = Query(1),
    order_id: str = Query(None),
    db: Session = Depends(get_db)
):
    rule_data = {
        'text': text,
        'user_id': user_id,
        'order_id': order_id
    }
    
    result = rule_engine.execute("after_sale_classification", rule_data)
    
    return APIResponse.success({
        "classification": result["rule_results"][0]["details"].get("classification", "OTHER"),
        "confidence": result["rule_results"][0]["details"].get("confidence", 0.0),
        "matched_keywords": result["rule_results"][0]["details"].get("matched_keywords", []),
        "message": result["message"]
    })

@router.post("/quality-inspect", response_model=APIResponse)
def quality_inspect(
    order_id: str = Query(None),
    user_id: int = Query(1),
    after_sale_type: str = Query(...),
    reason: str = Query(...),
    images: str = Query("[]"),
    product_info: str = Query(None),
    db: Session = Depends(get_db)
):
    import json
    images_list = []
    if images:
        try:
            images_list = json.loads(images)
        except:
            pass
    
    rule_data = {
        'order_id': order_id,
        'user_id': user_id,
        'after_sale_type': after_sale_type,
        'reason': reason,
        'images': images_list,
        'product_info': product_info
    }
    
    result = rule_engine.execute("quality_inspection", rule_data)
    
    return APIResponse.success({
        "quality_level": result["rule_results"][0]["details"].get("quality_level", "LOW"),
        "issues": result["rule_results"][0]["details"].get("issues", []),
        "warnings": result["rule_results"][0]["details"].get("warnings", []),
        "suggestion": result["rule_results"][0]["details"].get("suggestion", ""),
        "message": result["message"]
    })