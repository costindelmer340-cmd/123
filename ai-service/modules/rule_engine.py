"""
规则引擎模块 - 处理售后业务规则、质检审核、转人工判断等核心逻辑
"""
from abc import ABC, abstractmethod
from enum import Enum
from typing import List, Dict, Optional, Tuple, Any


class RuleResult(Enum):
    PASS = "pass"
    FAIL = "fail"
    PENDING = "pending"
    ESCALATE = "escalate"


class AfterSaleType(Enum):
    RETURN = "退货退款"
    EXCHANGE = "换货"
    REFUND = "仅退款"
    REPAIR = "维修"
    REISSUE = "补发"
    COMPENSATION = "赔偿"
    OTHER = "其他"


class QualityLevel(Enum):
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"


class Rule(ABC):
    """规则基类"""

    @abstractmethod
    def evaluate(self, data: Dict[str, Any]) -> Tuple[RuleResult, Dict[str, Any]]:
        """
        评估规则
        :param data: 输入数据
        :return: (规则结果, 详细信息)
        """
        pass

    @abstractmethod
    def get_name(self) -> str:
        """获取规则名称"""
        pass

    @abstractmethod
    def get_description(self) -> str:
        """获取规则描述"""
        pass


class RuleEngine:
    """规则引擎"""

    def __init__(self):
        self.rules: Dict[str, List[Rule]] = {}

    def register_rule(self, rule_set: str, rule: Rule):
        """
        注册规则
        :param rule_set: 规则集名称
        :param rule: 规则实例
        """
        if rule_set not in self.rules:
            self.rules[rule_set] = []
        self.rules[rule_set].append(rule)

    def execute(self, rule_set: str, data: Dict[str, Any]) -> Dict[str, Any]:
        """
        执行规则集
        :param rule_set: 规则集名称
        :param data: 输入数据
        :return: 执行结果
        """
        results = []
        overall_result = RuleResult.PASS
        failed_rules = []

        if rule_set not in self.rules:
            return {
                "overall_result": RuleResult.PENDING.value,
                "message": f"规则集 '{rule_set}' 不存在",
                "rule_results": []
            }

        for rule in self.rules[rule_set]:
            result, details = rule.evaluate(data)
            results.append({
                "rule_name": rule.get_name(),
                "rule_description": rule.get_description(),
                "result": result.value,
                "details": details
            })

            if result == RuleResult.FAIL:
                overall_result = RuleResult.FAIL
                failed_rules.append(rule.get_name())
            elif result == RuleResult.PENDING and overall_result == RuleResult.PASS:
                overall_result = RuleResult.PENDING
            elif result == RuleResult.ESCALATE:
                overall_result = RuleResult.ESCALATE
                failed_rules.append(rule.get_name())

        return {
            "overall_result": overall_result.value,
            "message": self._get_message(overall_result, failed_rules),
            "rule_results": results,
            "failed_rules": failed_rules
        }

    def _get_message(self, result: RuleResult, failed_rules: List[str]) -> str:
        """生成结果消息"""
        if result == RuleResult.PASS:
            return "所有规则验证通过"
        elif result == RuleResult.FAIL:
            return f"规则验证失败，失败规则：{', '.join(failed_rules)}"
        elif result == RuleResult.PENDING:
            return "规则验证待审核"
        elif result == RuleResult.ESCALATE:
            return f"需要转人工处理，触发规则：{', '.join(failed_rules)}"
        return "未知结果"


class AfterSaleClassificationRule(Rule):
    """售后分类规则"""

    RETURN_KEYWORDS = [
        '退货', '退', '不要了', '退掉', '退回去', '寄回', '退回',
        '不满意', '不喜欢', '不合适', '买错了', '发错了', '颜色错', '尺码错'
    ]
    EXCHANGE_KEYWORDS = [
        '换货', '换', '换一个', '换颜色', '换尺码', '换型号',
        '更换', '换件', '换新'
    ]
    REFUND_KEYWORDS = [
        '退款', '退钱', '返钱', '还钱', '退货款', '仅退款',
        '没收到', '未收到', '没发货', '迟迟不发', '缺货'
    ]
    REPAIR_KEYWORDS = [
        '维修', '修', '坏了', '故障', '不能用', '无法使用',
        '损坏', '坏的', '有问题', '故障', '失灵', '开不了机', '充不了电'
    ]
    REISSUE_KEYWORDS = [
        '补发', '重发', '再发', '补发商品', '重新发货',
        '漏发', '少发', '发少了', '缺件'
    ]
    COMPENSATION_KEYWORDS = [
        '赔偿', '补偿', '赔付', '赔钱', '损失费',
        '耽误时间', '精神损失', '违约金'
    ]

    def evaluate(self, data: Dict[str, Any]) -> Tuple[RuleResult, Dict[str, Any]]:
        text = data.get('text', '').lower()
        if not text:
            return RuleResult.PENDING, {"classification": AfterSaleType.OTHER.value, "confidence": 0.0, "matched_keywords": []}

        scores = {}
        scores[AfterSaleType.RETURN] = sum(1 for kw in self.RETURN_KEYWORDS if kw in text)
        scores[AfterSaleType.EXCHANGE] = sum(1 for kw in self.EXCHANGE_KEYWORDS if kw in text)
        scores[AfterSaleType.REFUND] = sum(1 for kw in self.REFUND_KEYWORDS if kw in text)
        scores[AfterSaleType.REPAIR] = sum(1 for kw in self.REPAIR_KEYWORDS if kw in text)
        scores[AfterSaleType.REISSUE] = sum(1 for kw in self.REISSUE_KEYWORDS if kw in text)
        scores[AfterSaleType.COMPENSATION] = sum(1 for kw in self.COMPENSATION_KEYWORDS if kw in text)

        max_score = max(scores.values())
        if max_score == 0:
            return RuleResult.PASS, {"classification": AfterSaleType.OTHER.value, "confidence": 0.0, "matched_keywords": []}

        classification = max(scores, key=scores.get)
        total_keywords = sum(len(kws) for kws in [
            self.RETURN_KEYWORDS, self.EXCHANGE_KEYWORDS, self.REFUND_KEYWORDS,
            self.REPAIR_KEYWORDS, self.REISSUE_KEYWORDS, self.COMPENSATION_KEYWORDS
        ])
        confidence = min(max_score / 5, 1.0)

        return RuleResult.PASS, {
            "classification": classification.value,
            "confidence": round(confidence, 2),
            "matched_keywords": [kw for kw in self._get_all_keywords() if kw in text],
            "scores": {k.value: v for k, v in scores.items() if v > 0}
        }

    def _get_all_keywords(self) -> List[str]:
        return self.RETURN_KEYWORDS + self.EXCHANGE_KEYWORDS + self.REFUND_KEYWORDS + \
               self.REPAIR_KEYWORDS + self.REISSUE_KEYWORDS + self.COMPENSATION_KEYWORDS

    def get_name(self) -> str:
        return "售后分类规则"

    def get_description(self) -> str:
        return "根据用户输入自动分类售后类型（退货/换货/退款/维修/补发/赔偿）"


class QualityInspectionRule(Rule):
    """质检规则 - 检查售后申请是否符合规范"""

    def evaluate(self, data: Dict[str, Any]) -> Tuple[RuleResult, Dict[str, Any]]:
        issues = []
        warnings = []

        order_id = data.get('order_id')
        if not order_id:
            issues.append("缺少订单号")

        user_id = data.get('user_id')
        if not user_id:
            issues.append("缺少用户ID")

        after_sale_type = data.get('after_sale_type')
        if not after_sale_type:
            issues.append("缺少售后类型")
        elif after_sale_type not in [t.value for t in AfterSaleType]:
            issues.append(f"无效的售后类型: {after_sale_type}")

        reason = data.get('reason', '')
        if not reason or len(reason.strip()) < 5:
            issues.append("售后原因描述过于简短")

        images = data.get('images', [])
        if after_sale_type in [AfterSaleType.RETURN.value, AfterSaleType.REPAIR.value, AfterSaleType.COMPENSATION.value]:
            if len(images) == 0:
                warnings.append("建议提供商品照片作为凭证")
            elif len(images) < 2:
                warnings.append("建议提供至少2张照片")

        product_info = data.get('product_info')
        if not product_info:
            warnings.append("缺少商品信息")

        if issues:
            return RuleResult.FAIL, {
                "issues": issues,
                "warnings": warnings,
                "quality_level": QualityLevel.LOW.value,
                "suggestion": "请补充完整必要信息后重新提交"
            }

        quality_level = QualityLevel.HIGH
        if warnings:
            quality_level = QualityLevel.MEDIUM

        return RuleResult.PASS, {
            "issues": issues,
            "warnings": warnings,
            "quality_level": quality_level.value,
            "suggestion": "申请信息完整，可进入审核流程"
        }

    def get_name(self) -> str:
        return "质检规则"

    def get_description(self) -> str:
        return "检查售后申请是否完整、符合规范"


class EscalationRule(Rule):
    """转人工规则"""

    ESCALATION_INTENTS = ['投诉', '举报', '欺诈', '威胁', '法律', '律师', '媒体']
    HIGH_RISK_KEYWORDS = ['垃圾', '骗子', '诈骗', '去死', '投诉到', '曝光', '媒体', '维权']
    COMPLAINT_KEYWORDS = ['投诉', '差评', '举报', '投诉', '不满意', '太差']

    def evaluate(self, data: Dict[str, Any]) -> Tuple[RuleResult, Dict[str, Any]]:
        intent = data.get('intent', '').lower()
        sentiment = data.get('sentiment', '').lower()
        risk_level = data.get('risk_level', '').lower()
        text = data.get('text', '').lower()

        triggers = []

        if any(kw in intent for kw in self.ESCALATION_INTENTS):
            triggers.append("意图包含投诉/举报等敏感词")

        if sentiment == 'negative':
            sentiment_score = data.get('sentiment_score', 0)
            if sentiment_score < 0.3:
                triggers.append("情感分析为强烈负面")

        if risk_level == 'high':
            triggers.append("风险等级为高")

        if any(kw in text for kw in self.HIGH_RISK_KEYWORDS):
            triggers.append("文本包含高风险关键词")

        if intent == '转人工' or '人工' in text or '真人' in text:
            triggers.append("用户明确要求转人工")

        issue_count = data.get('issue_count', 0)
        if issue_count >= 3:
            triggers.append("同一问题重复咨询超过3次")

        order_amount = data.get('order_amount', 0)
        if order_amount > 10000:
            triggers.append("订单金额超过10000元")

        if triggers:
            return RuleResult.ESCALATE, {
                "escalate": True,
                "triggers": triggers,
                "reason": "; ".join(triggers),
                "priority": "high" if len(triggers) >= 2 else "medium"
            }

        return RuleResult.PASS, {
            "escalate": False,
            "triggers": [],
            "reason": "未触发转人工规则",
            "priority": "normal"
        }

    def get_name(self) -> str:
        return "转人工规则"

    def get_description(self) -> str:
        return "根据意图、情感、风险等级等判断是否需要转人工处理"


class ReviewAnalysisRule(Rule):
    """评价分析规则 - 从用户评价中提取关键问题"""

    LOGISTICS_KEYWORDS = ['物流', '快递', '发货', '配送', '运输', '到货', '慢', '快', '准时']
    QUALITY_KEYWORDS = ['质量', '品质', '做工', '材料', '手感', '好用', '不好用', '坏', '烂']
    SERVICE_KEYWORDS = ['客服', '服务', '态度', '售后', '处理', '回复', '专业', '耐心']
    PRICE_KEYWORDS = ['价格', '贵', '便宜', '划算', '性价比', '优惠', '打折']
    PACKAGING_KEYWORDS = ['包装', '破损', '完好', '精美', '简陋']

    def evaluate(self, data: Dict[str, Any]) -> Tuple[RuleResult, Dict[str, Any]]:
        text = data.get('text', '')
        if not text:
            return RuleResult.PENDING, {"analysis": "无评价内容"}

        issues = []
        positive_points = []
        negative_points = []

        if any(kw in text for kw in self.LOGISTICS_KEYWORDS):
            if any(bad in text for bad in ['慢', '延迟', '没到', '晚', '久']):
                issues.append("物流问题")
                negative_points.append("物流速度慢")
            elif any(good in text for good in ['快', '准时', '及时']):
                positive_points.append("物流速度快")

        if any(kw in text for kw in self.QUALITY_KEYWORDS):
            if any(bad in text for bad in ['坏', '烂', '差', '粗糙', '有问题']):
                issues.append("商品质量问题")
                negative_points.append("商品质量差")
            elif any(good in text for good in ['好', '不错', '满意', '精致']):
                positive_points.append("商品质量好")

        if any(kw in text for kw in self.SERVICE_KEYWORDS):
            if any(bad in text for bad in ['态度差', '不理人', '不回复', '敷衍']):
                issues.append("服务态度问题")
                negative_points.append("服务态度差")
            elif any(good in text for good in ['专业', '耐心', '热情']):
                positive_points.append("服务态度好")

        if any(kw in text for kw in self.PRICE_KEYWORDS):
            if any(bad in text for bad in ['贵', '不值', '坑']):
                issues.append("价格争议")
                negative_points.append("价格过高")
            elif any(good in text for good in ['便宜', '划算', '性价比高']):
                positive_points.append("价格合理")

        if any(kw in text for kw in self.PACKAGING_KEYWORDS):
            if any(bad in text for bad in ['破损', '烂', '简陋']):
                issues.append("包装问题")
                negative_points.append("包装破损")
            elif any(good in text for good in ['完好', '精美']):
                positive_points.append("包装完好")

        rating = data.get('rating', 3)
        if rating <= 2:
            sentiment = "negative"
        elif rating == 3:
            sentiment = "neutral"
        else:
            sentiment = "positive"

        return RuleResult.PASS, {
            "sentiment": sentiment,
            "rating": rating,
            "issues": issues,
            "positive_points": positive_points,
            "negative_points": negative_points,
            "summary": self._generate_summary(sentiment, issues, positive_points, negative_points),
            "keywords": self._extract_keywords(text)
        }

    def _generate_summary(self, sentiment: str, issues: List[str],
                          positive: List[str], negative: List[str]) -> str:
        if sentiment == "negative":
            if issues:
                return f"负面评价，主要问题：{', '.join(issues)}"
            return "负面评价"
        elif sentiment == "positive":
            if positive:
                return f"正面评价，好评点：{', '.join(positive)}"
            return "正面评价"
        else:
            parts = []
            if positive:
                parts.append(f"优点：{', '.join(positive)}")
            if negative:
                parts.append(f"缺点：{', '.join(negative)}")
            if issues:
                parts.append(f"问题：{', '.join(issues)}")
            return "; ".join(parts) if parts else "中性评价"

    def _extract_keywords(self, text: str) -> List[str]:
        all_keywords = self.LOGISTICS_KEYWORDS + self.QUALITY_KEYWORDS + self.SERVICE_KEYWORDS + \
                       self.PRICE_KEYWORDS + self.PACKAGING_KEYWORDS
        return list(set(kw for kw in all_keywords if kw in text))

    def get_name(self) -> str:
        return "评价分析规则"

    def get_description(self) -> str:
        return "分析用户评价，提取关键问题和好评/差评点"


rule_engine = RuleEngine()
rule_engine.register_rule("after_sale_classification", AfterSaleClassificationRule())
rule_engine.register_rule("quality_inspection", QualityInspectionRule())
rule_engine.register_rule("escalation", EscalationRule())
rule_engine.register_rule("review_analysis", ReviewAnalysisRule())
rule_engine.register_rule("after_sale_full", AfterSaleClassificationRule())
rule_engine.register_rule("after_sale_full", QualityInspectionRule())
rule_engine.register_rule("after_sale_full", EscalationRule())