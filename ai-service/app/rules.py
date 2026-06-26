from collections.abc import Iterable


def contains_any(text: str, keywords: Iterable[str]) -> bool:
    normalized = text.lower()
    return any(keyword.lower() in normalized for keyword in keywords)


INTENT_RULES = [
    ("RETURN_REFUND", "退货退款", ["退货", "退款", "退钱", "退回", "不想要", "七天无理由"]),
    ("EXCHANGE", "换货", ["换货", "换一个", "更换", "尺码不合适", "颜色不对"]),
    ("REPAIR", "维修", ["维修", "修理", "坏了", "不能用", "故障", "开不了机"]),
    ("LOGISTICS", "物流查询", ["物流", "快递", "发货", "到哪", "没收到", "签收"]),
    ("COMPLAINT", "投诉", ["投诉", "差评", "举报", "欺骗", "态度差"]),
]

TOPIC_RULES = {
    "商品质量": ["质量", "坏了", "划痕", "破损", "故障", "不能用", "瑕疵"],
    "退款时效": ["退款", "到账", "退钱", "多久"],
    "物流配送": ["物流", "快递", "发货", "没收到", "签收", "运输"],
    "客服体验": ["客服", "态度", "回复", "服务"],
    "平台规则": ["七天无理由", "规则", "政策", "凭证", "审核"],
}

NEGATIVE_WORDS = [
    "差",
    "坏",
    "慢",
    "太慢",
    "回复慢",
    "投诉",
    "生气",
    "失望",
    "欺骗",
    "破损",
    "划痕",
    "不能用",
    "没收到",
    "退货",
    "退款",
]
POSITIVE_WORDS = ["好", "满意", "及时", "快", "耐心", "感谢", "不错", "负责"]
