from fastapi import APIRouter, Query, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.common.response import APIResponse
from datetime import datetime
import uuid

router = APIRouter(prefix="/ticket", tags=["工单管理"])

_tickets = {}

@router.post("/create", response_model=APIResponse)
def create_ticket(
    user_id: int = Query(...),
    type: str = Query(...),
    title: str = Query(...),
    content: str = Query(...),
    order_id: int = Query(None),
    after_sale_no: str = Query(None),
    evidence_urls: str = Query(None),
    priority: str = Query("NORMAL"),
    db: Session = Depends(get_db)
):
    ticket_id = f"T{datetime.now().strftime('%Y%m%d%H%M%S')}{uuid.uuid4().hex[:4].upper()}"
    
    ticket = {
        "ticket_id": ticket_id,
        "user_id": user_id,
        "type": type,
        "title": title,
        "content": content,
        "status": "PENDING",
        "priority": priority,
        "order_id": order_id,
        "after_sale_no": after_sale_no,
        "evidence_urls": evidence_urls.split(",") if evidence_urls else [],
        "created_at": datetime.now().isoformat(),
        "updated_at": datetime.now().isoformat(),
        "assignee": None,
        "history": [{
            "time": datetime.now().isoformat(),
            "action": "创建工单",
            "operator": "系统",
            "description": "工单已创建，等待处理"
        }]
    }
    
    _tickets[ticket_id] = ticket
    
    estimated_time = "1-2工作日"
    if priority == "HIGH":
        estimated_time = "24小时内"
    elif priority == "URGENT":
        estimated_time = "4小时内"
    
    return APIResponse.success({
        "ticket_id": ticket_id,
        "user_id": user_id,
        "type": type,
        "title": title,
        "status": "PENDING",
        "priority": priority,
        "created_at": ticket["created_at"],
        "estimated_time": estimated_time
    })

@router.get("/list", response_model=APIResponse)
def get_ticket_list(
    user_id: int = Query(None),
    status: str = Query(None),
    type: str = Query(None),
    priority: str = Query(None),
    limit: int = Query(20),
    offset: int = Query(0),
    db: Session = Depends(get_db)
):
    filtered = list(_tickets.values())
    
    if user_id:
        filtered = [t for t in filtered if t["user_id"] == user_id]
    if status:
        filtered = [t for t in filtered if t["status"] == status]
    if type:
        filtered = [t for t in filtered if t["type"] == type]
    if priority:
        filtered = [t for t in filtered if t["priority"] == priority]
    
    filtered.sort(key=lambda x: x["created_at"], reverse=True)
    
    return APIResponse.success({
        "list": filtered[offset:offset+limit],
        "total": len(filtered),
        "limit": limit,
        "offset": offset
    })

@router.get("/detail", response_model=APIResponse)
def get_ticket_detail(
    ticket_id: str = Query(...),
    db: Session = Depends(get_db)
):
    ticket = _tickets.get(ticket_id)
    
    if not ticket:
        return APIResponse.error(404, "工单不存在")
    
    return APIResponse.success(ticket)

@router.put("/update", response_model=APIResponse)
def update_ticket(
    ticket_id: str = Query(...),
    status: str = Query(None),
    assignee: str = Query(None),
    remark: str = Query(None),
    priority: str = Query(None),
    db: Session = Depends(get_db)
):
    ticket = _tickets.get(ticket_id)
    
    if not ticket:
        return APIResponse.error(404, "工单不存在")
    
    updates = []
    
    if status and status != ticket["status"]:
        ticket["status"] = status
        updates.append(f"状态变更为{status}")
    
    if assignee and assignee != ticket["assignee"]:
        ticket["assignee"] = assignee
        updates.append(f"指派给{assignee}")
    
    if remark:
        updates.append(f"备注: {remark}")
    
    if priority and priority != ticket["priority"]:
        ticket["priority"] = priority
        updates.append(f"优先级变更为{priority}")
    
    ticket["updated_at"] = datetime.now().isoformat()
    
    if updates:
        ticket["history"].append({
            "time": datetime.now().isoformat(),
            "action": "更新工单",
            "operator": "系统",
            "description": "; ".join(updates)
        })
    
    return APIResponse.success({
        "ticket_id": ticket_id,
        "status": ticket["status"],
        "assignee": ticket["assignee"],
        "priority": ticket["priority"],
        "remark": remark,
        "updated_at": ticket["updated_at"]
    })

@router.put("/close", response_model=APIResponse)
def close_ticket(
    ticket_id: str = Query(...),
    close_reason: str = Query("用户主动关闭"),
    resolution: str = Query(None),
    db: Session = Depends(get_db)
):
    ticket = _tickets.get(ticket_id)
    
    if not ticket:
        return APIResponse.error(404, "工单不存在")
    
    ticket["status"] = "CLOSED"
    ticket["close_reason"] = close_reason
    ticket["resolution"] = resolution
    ticket["closed_at"] = datetime.now().isoformat()
    ticket["updated_at"] = datetime.now().isoformat()
    
    ticket["history"].append({
        "time": datetime.now().isoformat(),
        "action": "关闭工单",
        "operator": "系统",
        "description": f"工单已关闭，原因: {close_reason}" + (f"，解决方案: {resolution}" if resolution else "")
    })
    
    return APIResponse.success({
        "ticket_id": ticket_id,
        "status": "CLOSED",
        "close_reason": close_reason,
        "resolution": resolution,
        "closed_at": ticket["closed_at"]
    })

@router.post("/transfer", response_model=APIResponse)
def transfer_ticket(
    ticket_id: str = Query(...),
    target_assignee: str = Query(...),
    reason: str = Query("需要其他同事协助处理"),
    db: Session = Depends(get_db)
):
    ticket = _tickets.get(ticket_id)
    
    if not ticket:
        return APIResponse.error(404, "工单不存在")
    
    old_assignee = ticket["assignee"]
    ticket["assignee"] = target_assignee
    ticket["updated_at"] = datetime.now().isoformat()
    
    ticket["history"].append({
        "time": datetime.now().isoformat(),
        "action": "工单转接",
        "operator": old_assignee or "系统",
        "description": f"由{old_assignee or '系统'}转交给{target_assignee}，原因: {reason}"
    })
    
    return APIResponse.success({
        "ticket_id": ticket_id,
        "from_assignee": old_assignee,
        "to_assignee": target_assignee,
        "reason": reason,
        "updated_at": ticket["updated_at"]
    })

@router.get("/stats", response_model=APIResponse)
def get_ticket_stats(
    start_date: str = Query(None),
    end_date: str = Query(None),
    db: Session = Depends(get_db)
):
    tickets = list(_tickets.values())
    
    stats = {
        "total": len(tickets),
        "pending": sum(1 for t in tickets if t["status"] == "PENDING"),
        "processing": sum(1 for t in tickets if t["status"] == "PROCESSING"),
        "resolved": sum(1 for t in tickets if t["status"] == "RESOLVED"),
        "closed": sum(1 for t in tickets if t["status"] == "CLOSED"),
        "by_type": {},
        "by_priority": {}
    }
    
    for ticket in tickets:
        stats["by_type"][ticket["type"]] = stats["by_type"].get(ticket["type"], 0) + 1
        stats["by_priority"][ticket["priority"]] = stats["by_priority"].get(ticket["priority"], 0) + 1
    
    return APIResponse.success(stats)

@router.post("/comment", response_model=APIResponse)
def add_comment(
    ticket_id: str = Query(...),
    comment: str = Query(...),
    operator: str = Query("客服"),
    db: Session = Depends(get_db)
):
    ticket = _tickets.get(ticket_id)
    
    if not ticket:
        return APIResponse.error(404, "工单不存在")
    
    if "comments" not in ticket:
        ticket["comments"] = []
    
    comment_item = {
        "time": datetime.now().isoformat(),
        "operator": operator,
        "content": comment
    }
    
    ticket["comments"].append(comment_item)
    ticket["updated_at"] = datetime.now().isoformat()
    
    ticket["history"].append({
        "time": datetime.now().isoformat(),
        "action": "添加备注",
        "operator": operator,
        "description": f"添加备注: {comment[:20]}..." if len(comment) > 20 else f"添加备注: {comment}"
    })
    
    return APIResponse.success({
        "ticket_id": ticket_id,
        "comment": comment_item,
        "total_comments": len(ticket["comments"])
    })