import mysql.connector
from app.config import settings


def get_order_info(order_no: str) -> dict | None:
    try:
        cnx = mysql.connector.connect(
            host=settings.db_host,
            port=settings.db_port,
            user=settings.db_user,
            password=settings.db_password,
            database=settings.db_name,
            charset="utf8mb4"
        )
        cursor = cnx.cursor(dictionary=True)
        
        cursor.execute("""
            SELECT o.order_no, o.order_status, o.pay_status, o.logistics_status, 
                   o.after_sale_status, o.total_amount, o.paid_at, o.delivered_at,
                   i.product_name, i.sku_name, i.quantity, i.unit_price
            FROM twenty_mall_order o
            LEFT JOIN twenty_mall_order_item i ON o.id = i.order_id AND i.deleted = 0
            WHERE o.order_no = %s AND o.deleted = 0
            LIMIT 1
        """, (order_no,))
        
        result = cursor.fetchone()
        cursor.close()
        cnx.close()
        
        if result:
            return {
                "order_no": result.get("order_no"),
                "order_status": result.get("order_status"),
                "pay_status": result.get("pay_status"),
                "logistics_status": result.get("logistics_status"),
                "after_sale_status": result.get("after_sale_status"),
                "total_amount": str(result.get("total_amount", 0)),
                "product_name": result.get("product_name"),
                "sku_name": result.get("sku_name"),
                "quantity": result.get("quantity", 0),
                "unit_price": str(result.get("unit_price", 0)),
                "paid_at": str(result.get("paid_at")) if result.get("paid_at") else None,
                "delivered_at": str(result.get("delivered_at")) if result.get("delivered_at") else None,
            }
        return None
    except Exception:
        return None


def get_after_sale_info(order_no: str) -> list[dict]:
    try:
        cnx = mysql.connector.connect(
            host=settings.db_host,
            port=settings.db_port,
            user=settings.db_user,
            password=settings.db_password,
            database=settings.db_name,
            charset="utf8mb4"
        )
        cursor = cnx.cursor(dictionary=True)
        
        cursor.execute("""
            SELECT a.after_sale_no, a.after_sale_type, a.reason_type, 
                   a.description, a.requested_amount, a.status, a.created_at
            FROM twenty_mall_after_sale a
            JOIN twenty_mall_order o ON a.order_id = o.id
            WHERE o.order_no = %s AND a.deleted = 0 AND o.deleted = 0
            ORDER BY a.created_at DESC
        """, (order_no,))
        
        results = cursor.fetchall()
        cursor.close()
        cnx.close()
        
        return [
            {
                "after_sale_no": item.get("after_sale_no"),
                "after_sale_type": item.get("after_sale_type"),
                "reason_type": item.get("reason_type"),
                "description": item.get("description"),
                "requested_amount": str(item.get("requested_amount", 0)),
                "status": item.get("status"),
                "created_at": str(item.get("created_at")) if item.get("created_at") else None,
            }
            for item in results
        ]
    except Exception:
        return []


def extract_order_no(text: str) -> str | None:
    import re
    patterns = [
        r'订单号[：:]?\s*([A-Za-z0-9]+)',
        r'订单\s*([A-Za-z0-9]+)',
        r'([A-Za-z]{2}[0-9]{10,})',
        r'([0-9]{10,})',
    ]
    for pattern in patterns:
        match = re.search(pattern, text)
        if match:
            return match.group(1)
    return None


def search_order_by_product(text: str) -> list[dict]:
    try:
        cnx = mysql.connector.connect(
            host=settings.db_host,
            port=settings.db_port,
            user=settings.db_user,
            password=settings.db_password,
            database=settings.db_name,
            charset="utf8mb4"
        )
        cursor = cnx.cursor(dictionary=True)
        
        cleaned_text = ''.join([c for c in text if '\u4e00' <= c <= '\u9fa5' or c.isalnum()])
        if len(cleaned_text) < 1:
            return []
        
        search_pattern = f"%{cleaned_text}%"
        
        char_patterns = [f"%{c}%" for c in cleaned_text]
        
        char_conditions = []
        params = []
        
        for pattern in char_patterns:
            char_conditions.append("i.product_name LIKE %s")
            params.append(pattern)
            char_conditions.append("i.sku_name LIKE %s")
            params.append(pattern)
        
        char_clause = " OR ".join(char_conditions)
        
        cursor.execute(f"""
            SELECT DISTINCT o.order_no, o.order_status, o.pay_status, o.logistics_status, 
                   o.after_sale_status, o.total_amount,
                   i.product_name, i.sku_name, i.quantity, i.unit_price,
                   o.created_at
            FROM twenty_mall_order o
            LEFT JOIN twenty_mall_order_item i ON o.id = i.order_id AND i.deleted = 0
            WHERE o.deleted = 0 AND (
                i.product_name LIKE %s OR 
                i.sku_name LIKE %s OR
                o.order_no LIKE %s OR
                {char_clause}
            )
            ORDER BY o.created_at DESC
            LIMIT 10
        """, (search_pattern, search_pattern, search_pattern) + tuple(params))
        
        results = cursor.fetchall()
        cursor.close()
        cnx.close()
        
        return [
            {
                "order_no": item.get("order_no"),
                "order_status": item.get("order_status"),
                "pay_status": item.get("pay_status"),
                "logistics_status": item.get("logistics_status"),
                "after_sale_status": item.get("after_sale_status"),
                "total_amount": str(item.get("total_amount", 0)),
                "product_name": item.get("product_name"),
                "sku_name": item.get("sku_name"),
                "quantity": item.get("quantity", 0),
                "unit_price": str(item.get("unit_price", 0)),
                "created_at": str(item.get("created_at")) if item.get("created_at") else None,
            }
            for item in results
        ]
    except Exception:
        return []


def extract_keywords(text: str) -> list[str]:
    import re
    stop_words = {
        "的", "了", "是", "我", "你", "他", "她", "它", "在", "有", "和", "就", "不", "人", "都", 
        "一", "上", "也", "很", "到", "说", "要", "去", "会", "着", "没有", "看", "好", 
        "自己", "这", "那", "这个", "那个", "什么", "怎么", "多少", "一些", "一点", "有点", 
        "可以", "能", "不能", "想", "不想", "需要", "不需要", "订单", "商品", "买", "卖", 
        "退款", "退货", "换货", "物流", "快递", "发货", "签收", "售后", "申请", "问题", 
        "客服", "咨询", "请问", "你好", "您好", "谢谢", "感谢", "麻烦", "帮我", "关于", 
        "一下", "啦", "啊", "哦", "呢", "吧", "嘛", "呗", "嗯", "唔", "噢", "呃", "呀", 
        "哇", "哈", "嘿", "哼", "呵", "嘻", "咦", "查询", "确认", "处理", "办理", 
        "了解", "知道", "看看", "查一下", "问一下", "谁", "什么时候", "哪里", "为什么", 
        "怎么样", "如何", "应该", "不应该", "不会", "不要"
    }
    
    text = re.sub(r'[^\u4e00-\u9fa5a-zA-Z0-9]', '', text)
    
    keywords = []
    i = 0
    while i < len(text):
        if text[i] in stop_words:
            i += 1
            continue
        
        matched = False
        for length in [4, 3, 2]:
            if i + length <= len(text):
                candidate = text[i:i+length]
                if candidate not in stop_words:
                    has_stop = False
                    for j in range(length):
                        if candidate[j] in stop_words:
                            has_stop = True
                            break
                    if not has_stop:
                        keywords.append(candidate)
                        i += length
                        matched = True
                        break
        
        if not matched:
            if text[i] not in stop_words:
                keywords.append(text[i])
            i += 1
    
    seen = set()
    result = []
    for kw in keywords:
        if kw not in seen and kw not in stop_words and len(kw) >= 1:
            seen.add(kw)
            result.append(kw)
    
    return result[:10]


def build_context(order_no: str) -> str:
    order_info = get_order_info(order_no)
    after_sale_info = get_after_sale_info(order_no)
    
    if not order_info:
        return ""
    
    context = f"订单信息：\n"
    context += f"- 订单号：{order_info['order_no']}\n"
    context += f"- 商品名称：{order_info['product_name']}\n"
    context += f"- 规格：{order_info['sku_name']}\n"
    context += f"- 数量：{order_info['quantity']}\n"
    context += f"- 单价：¥{order_info['unit_price']}\n"
    context += f"- 订单金额：¥{order_info['total_amount']}\n"
    context += f"- 订单状态：{order_info['order_status']}\n"
    context += f"- 支付状态：{order_info['pay_status']}\n"
    context += f"- 物流状态：{order_info['logistics_status']}\n"
    context += f"- 售后状态：{order_info['after_sale_status']}\n"
    
    if order_info['paid_at']:
        context += f"- 支付时间：{order_info['paid_at']}\n"
    if order_info['delivered_at']:
        context += f"- 签收时间：{order_info['delivered_at']}\n"
    
    if after_sale_info:
        context += "\n售后申请记录：\n"
        for i, item in enumerate(after_sale_info, 1):
            context += f"{i}. 售后单号：{item['after_sale_no']}\n"
            context += f"   类型：{item['after_sale_type']}\n"
            context += f"   原因：{item['reason_type']}\n"
            context += f"   描述：{item['description']}\n"
            context += f"   申请金额：¥{item['requested_amount']}\n"
            context += f"   状态：{item['status']}\n"
            context += f"   创建时间：{item['created_at']}\n"
    
    return context