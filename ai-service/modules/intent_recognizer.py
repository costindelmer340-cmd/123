"""
AI引擎 - 意图识别模块
识别用户query的意图类型
"""
import re
from typing import List, Dict, Tuple
from dataclasses import dataclass
from enum import Enum


class IntentType(Enum):
    """意图类型枚举"""
    LOGISTICS_DELAY = "物流延迟"           # 物流相关
    RETURN_REQUEST = "退货退款"            # 退换货
    PRODUCT_QUALITY = "商品质量"          # 质量投诉
    INVOICE_ISSUE = "发票问题"            # 发票相关
    ORDER_INQUIRY = "订单咨询"            # 订单查询
    REFUND_STATUS = "退款进度"            # 退款查询
    PRODUCT_DAMAGE = "商品损坏"           # 损坏投诉
    SERVICE_ATTITUDE = "服务态度"         # 服务投诉
    DELIVERY_ISSUE = "配送问题"           # 配送问题
    PRICE_ISSUE = "价格问题"              # 价格争议
    EXCHANGE_REQUEST = "换货申请"         # 换货
    CANCELLATION = "取消订单"             # 取消
    COMPLAINT = "投诉建议"               # 投诉
    GENERAL = "一般咨询"                 # 通用咨询
    GREETING = "问候"                    # 问候
    THANKS = "感谢"                      # 感谢
    ESCALATE = "转人工"                  # 转人工


@dataclass
class IntentResult:
    """意图识别结果"""
    primary_intent: IntentType
    confidence: float
    secondary_intents: List[Tuple[IntentType, float]]
    keywords: List[str]
    requires_escalation: bool


class IntentRecognizer:
    """意图识别器"""

    def __init__(self):
        # 意图关键词库
        self.intent_patterns = {
            IntentType.LOGISTICS_DELAY: [
                r'物流.*(慢|延迟|延迟|没到|未到|未收到)',
                r'(还没|还未|仍然).*(到|收到)',
                r'快递.*(慢|延迟|停|不动|没到)',
                r'(订单|包裹).*(多久|几天|多长时间).*(到|到货|收到)',
                r'发货.*(慢|延迟)',
                r'等.*(好久|多久|很长时间)',
                r'迟迟.*(没有|未|没)',
                r'发货.*(催促|催)',
                r'快递.*还没到',
                r'快递.*怎么还没',
                r'包裹.*没到',
                r'物流.*什么时候到',
                r'什么时候.*到货',
            ],
            IntentType.RETURN_REQUEST: [
                r'(退货|退款|退|退还)',
                r'(不要了|不想要了|不想要)',
                r'(取消|撤销).*(订单|交易)',
                r'(后悔|改变主意).*(购买|订单)',
                r'(尺寸|大小|颜色|款式).*(不合适|不对|不喜欢)',
            ],
            IntentType.PRODUCT_QUALITY: [
                r'(质量|品质).*(问题|差|不好|糟糕)',
                r'(损坏|破损|坏了|碎了|裂了)',
                r'(功能|使用).*(异常|不行|不能用|坏了)',
                r'(描述|图片).*(不符|不一样|差距)',
                r'(假货|仿冒|山寨|盗版)',
                r'(异味|味道|材质).*(有问题|不对)',
            ],
            IntentType.INVOICE_ISSUE: [
                r'(发票|收据).*(开具|开|需要|要)',
                r'(发票|收据).*(补开|重开|更换)',
                r'(发票类型|普票|专票|增票)',
                r'(报账|报销).*(需要发票)',
                r'(税金|税点)',
            ],
            IntentType.ORDER_INQUIRY: [
                r'(订单|单号).*(查询|查看|怎么找)',
                r'(订单|交易).*(状态|情况)',
                r'(什么时候|多久).*(发货|到货)',
                r'(订单|交易)号',
                r'(查|查看|查询).*(订单|我的订单)',
            ],
            IntentType.REFUND_STATUS: [
                r'(退款|钱).*(退.*哪|到.*哪|多久到账)',
                r'(退款|钱).*(进度|状态)',
                r'(退款|钱).*(没到|未到账|还没)',
                r'(申请退款|退款申请).*(多久|什么时候)',
                r'(退款|钱).*(审核|审批)',
            ],
            IntentType.PRODUCT_DAMAGE: [
                r'(收到|打开|拆开).*(发现|看到|就).*(损坏|破损|碎|裂)',
                r'(运输|快递).*(过程|途中|中).*(损坏|破)',
                r'(外包装|包装).*(破损|变形|损坏)',
                r'(商品|东西).*(压坏|摔坏|碰坏)',
            ],
            IntentType.SERVICE_ATTITUDE: [
                r'(客服|服务|态度).*(差|不好|恶劣|不满意)',
                r'(不理|不回|不回消息)',
                r'(敷衍|推诿|拖延)',
                r'(投诉|举报).*(客服|服务)',
            ],
            IntentType.DELIVERY_ISSUE: [
                r'(快递|配送|送货).*(不送|送不到|上门)',
                r'(地址|地址).*(修改|变更|更改)',
                r'(快递|物流).*(员|小哥|配送员).*(问题|态度)',
                r'(周末|节假日|晚上).*(能.*送|配送)',
            ],
            IntentType.PRICE_ISSUE: [
                r'(价格|价钱|费用).*(贵|高|不合理)',
                r'(降价|便宜|优惠)',
                r'(差价|价格差)',
                r'(能不能|可以.*优惠|打折)',
                r'(优惠券|红包|抵扣)',
            ],
            IntentType.EXCHANGE_REQUEST: [
                r'(换货|更换|换一个)',
                r'(重新|换个).*(商品|型号|颜色|尺寸)',
                r'(换.*个|换.*件)',
            ],
            IntentType.CANCELLATION: [
                r'(取消|撤销|关闭).*(订单|交易|购买)',
                r'(不.*买|不想.*买|不要.*了)',
                r'(后悔|改变主意)',
            ],
            IntentType.COMPLAINT: [
                r'(投诉|举报|差评|曝光)',
                r'(严重|强烈).*(不满|不满|抗议)',
                r'(要求|必须).*(处理|赔偿|补偿)',
                r'(曝光|发帖|媒体)',
            ],
            IntentType.GREETING: [
                r'^(你好|您好|hi|hello|嗨)',
                r'(早上|下午|晚上)好',
                r'在.*?',
            ],
            IntentType.THANKS: [
                r'(谢谢|感谢|辛苦了|麻烦)',
            ],
            IntentType.ESCALATE: [
                r'(转人工|人工|真人|客服)',
                r'(要.*投诉|我要.*举报)',
                r'(解.*不了|无法.*解决)',
                r'(上级|领导|经理|主管)',
            ],
        }

        # 意图权重（某些意图优先级更高）
        self.intent_weights = {
            IntentType.COMPLAINT: 1.0,
            IntentType.SERVICE_ATTITUDE: 1.0,
            IntentType.ESCALATE: 1.0,
            IntentType.PRODUCT_DAMAGE: 0.9,
            IntentType.LOGISTICS_DELAY: 0.8,
            IntentType.RETURN_REQUEST: 0.8,
            IntentType.PRODUCT_QUALITY: 0.8,
        }

        # 需要转人工的意图
        self.escalate_intents = {
            IntentType.COMPLAINT,
            IntentType.SERVICE_ATTITUDE,
            IntentType.ESCALATE,
            IntentType.PRODUCT_QUALITY,  # 严重质量问题
            IntentType.PRICE_ISSUE,       # 价格争议
        }

        # 编译正则表达式
        self._compile_patterns()

    def _compile_patterns(self):
        """编译所有正则表达式"""
        self.compiled_patterns = {}
        for intent, patterns in self.intent_patterns.items():
            self.compiled_patterns[intent] = [
                re.compile(p, re.IGNORECASE) for p in patterns
            ]

    def recognize(self, query: str) -> IntentResult:
        """
        识别用户query的意图

        Args:
            query: 用户输入的文本

        Returns:
            IntentResult: 意图识别结果
        """
        query = query.strip()
        if not query:
            return IntentResult(
                primary_intent=IntentType.GENERAL,
                confidence=0.0,
                secondary_intents=[],
                keywords=[],
                requires_escalation=False
            )

        # 统计每个意图的匹配次数和总分
        intent_scores: Dict[IntentType, Tuple[int, float]] = {}

        for intent, patterns in self.compiled_patterns.items():
            matches = 0
            total_score = 0.0

            for pattern in patterns:
                match = pattern.search(query)
                if match:
                    matches += 1
                    # 根据匹配的具体程度计算分数
                    score = 1.0
                    # 精确匹配权重更高
                    if pattern.match(query):
                        score = 1.5
                    # 包含关键词的匹配
                    if match.group():
                        score *= (len(match.group()) / len(query) + 0.5)

                    total_score += score

            if matches > 0:
                # 计算最终分数 = 匹配次数 * 平均分数 * 意图权重
                avg_score = total_score / matches
                weight = self.intent_weights.get(intent, 0.7)
                final_score = matches * avg_score * weight
                intent_scores[intent] = (matches, final_score)

        # 按分数排序
        sorted_intents = sorted(
            intent_scores.items(),
            key=lambda x: x[1][1],
            reverse=True
        )

        if not sorted_intents:
            return IntentResult(
                primary_intent=IntentType.GENERAL,
                confidence=0.5,
                secondary_intents=[],
                keywords=self._extract_keywords(query),
                requires_escalation=False
            )

        # 获取主要意图
        primary_intent = sorted_intents[0][0]
        primary_score = sorted_intents[0][1][1]

        # 计算置信度（归一化）
        max_possible_score = len(query) / 5  # 粗略估计最大分数
        confidence = min(primary_score / max_possible_score, 1.0) if max_possible_score > 0 else 0.5

        # 获取次要意图
        secondary_intents = []
        if primary_score > 0:
            secondary_intents = [
                (intent, score[1] / primary_score)
                for intent, score in sorted_intents[1:5]
                if score[1] / primary_score > 0.3
            ]

        # 判断是否需要转人工
        requires_escalation = (
            primary_intent in self.escalate_intents or
            (confidence > 0.8 and primary_score > 5)
        )

        return IntentResult(
            primary_intent=primary_intent,
            confidence=confidence,
            secondary_intents=secondary_intents,
            keywords=self._extract_keywords(query),
            requires_escalation=requires_escalation
        )

    def _extract_keywords(self, query: str) -> List[str]:
        """提取关键词"""
        # 简单实现：提取长度>=2的连续中文字符
        keywords = re.findall(r'[\u4e00-\u9fa5]{2,}', query)
        # 过滤常见停用词
        stopwords = {'这个', '那个', '请问', '你好', '我想', '我要', '可以', '帮我', '请问'}
        return [k for k in keywords if k not in stopwords][:5]


# 全局实例
intent_recognizer = IntentRecognizer()
