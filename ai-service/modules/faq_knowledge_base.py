"""
AI引擎 - FAQ知识库模块
本地FAQ精确匹配（降级方案）
"""
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass
import re


@dataclass
class FAQItem:
    """FAQ条目"""
    id: str
    question: str
    answer: str
    category: str
    tags: List[str]
    priority: int = 0
    view_count: int = 0


@dataclass
class FAQSearchResult:
    """FAQ搜索结果"""
    faq: FAQItem
    match_score: float
    match_type: str  # exact, keyword, fuzzy


class FAQKnowledgeBase:
    """FAQ知识库"""

    def __init__(self):
        # 内存存储FAQ
        self.faqs: Dict[str, FAQItem] = {}
        # 关键词索引
        self.keyword_index: Dict[str, List[str]] = {}

        # 初始化示例FAQ
        self._init_sample_faqs()

    def _init_sample_faqs(self):
        """初始化示例FAQ"""
        sample_faqs = [
            # 物流类
            FAQItem(
                id="logistics_001",
                question="物流一般多久到？",
                answer="正常情况下，商品将在发货后3-7天内送达。具体送达时间取决于您的收货地址和物流情况。您可以在订单详情中查看物流信息。",
                category="物流配送",
                tags=["物流", "时间", "到货"],
                priority=10
            ),
            FAQItem(
                id="logistics_002",
                question="物流延迟怎么办？",
                answer="如果您的物流有延迟，请稍安勿躁。您可以：1）联系客服催单；2）等待1-2天后再次查看物流信息。如超过10天未到达，请联系我们协助查询。",
                category="物流配送",
                tags=["物流", "延迟", "催单"],
                priority=8
            ),
            FAQItem(
                id="logistics_003",
                question="快递丢件怎么处理？",
                answer="如果您的包裹丢失，请立即联系客服并提供订单信息。我们会：1）核实物流信息；2）联系快递公司查找；3）如确认丢失，将为您安排退款或重新发货。",
                category="物流配送",
                tags=["快递", "丢件", "丢失"],
                priority=9
            ),

            # 退货退款类
            FAQItem(
                id="return_001",
                question="如何申请退货？",
                answer='退货流程：1）进入"我的订单"，找到需要退货的订单；2）点击"申请售后"；3）选择退货原因并提交；4）审核通过后，按照提示将商品寄回；5）收到商品后，退款将在7个工作日内原路返回。',
                category="退货退款",
                tags=["退货", "退款", "流程"],
                priority=10
            ),
            FAQItem(
                id="return_002",
                question="退货需要自己承担运费吗？",
                answer="退货运费的承担规则：1）商品存在质量问题/发错货，由商家承担运费；2）7天无理由退货，由买家承担运费；3）因个人原因（不喜欢/不合适）退货，如商品有运费险可申请赔付。具体请查看商品详情页的退换说明。",
                category="退货退款",
                tags=["退货", "运费", "谁承担"],
                priority=9
            ),
            FAQItem(
                id="return_003",
                question="退款多久到账？",
                answer="退款到账时间：1）原路退回：1-7个工作日（视支付渠道而定）；2）退至账户余额：即时到账；3）银行卡退款可能需要3-5个工作日。如超过7个工作日未到账，请联系客服查询。",
                category="退货退款",
                tags=["退款", "到账", "时间"],
                priority=8
            ),

            # 商品质量类
            FAQItem(
                id="quality_001",
                question="收到商品损坏怎么办？",
                answer="收到损坏商品的处理方法：1）请在收货后24小时内拍照留证；2）联系客服并上传照片；3）我们会根据损坏程度为您安排退货、退款或补发。请保留商品及包装以便核实。",
                category="商品质量",
                tags=["商品", "损坏", "破损"],
                priority=10
            ),
            FAQItem(
                id="quality_002",
                question="商品与描述不符怎么办？",
                answer="如收到的商品与页面描述不符，请：1）对比商品详情页描述；2）拍照留存；3）联系客服说明情况；4）提供对比照片；5）根据实际情况为您办理退货、退款或部分退款。",
                category="商品质量",
                tags=["描述不符", "商品", "差异"],
                priority=9
            ),

            # 发票类
            FAQItem(
                id="invoice_001",
                question="如何申请开发票？",
                answer='申请发票方式：1）订单完成后，在"申请开票"页面提交；2）支持普通发票和增值税专用发票；3）电子发票将在1-3个工作日内发送至您的邮箱；4）纸质发票将在7个工作日内寄出。',
                category="发票问题",
                tags=["发票", "开票", "申请"],
                priority=8
            ),
            FAQItem(
                id="invoice_002",
                question="发票可以补开吗？",
                answer='可以补开发票，请在订单完成后90天内申请。补开发票流程：1）进入"我的订单"；2）找到对应订单；3）点击"补开发票"；4）填写发票信息并提交。注意：已过期的订单可能无法补开。',
                category="发票问题",
                tags=["发票", "补开", "补打"],
                priority=7
            ),

            # 售后服务类
            FAQItem(
                id="service_001",
                question="商品有保修期吗？",
                answer="我们的商品均享有售后保障：1）7天无理由退换货；2）15天质量问题退换货；3）1年内免费维修（质量问题）。具体保修条款请查看商品详情页的售后服务说明。",
                category="售后服务",
                tags=["保修", "售后", "维修"],
                priority=8
            ),
            FAQItem(
                id="service_002",
                question="如何联系人工客服？",
                answer='联系人工客服的方式：1）APP内点击"客服"进入在线客服；2）拨打客服热线：400-XXX-XXXX（9:00-21:00）；3）微信公众号留言。我们会在24小时内回复您。',
                category="售后服务",
                tags=["客服", "联系", "人工"],
                priority=10
            ),
        ]

        # 添加到知识库
        for faq in sample_faqs:
            self.add_faq(faq)

    def add_faq(self, faq: FAQItem):
        """添加FAQ到知识库"""
        self.faqs[faq.id] = faq

        # 更新关键词索引
        faq_keywords = faq.tags + self._extract_keywords_from_question(faq.question)
        for keyword in faq_keywords:
            if len(keyword) >= 2:  # 只索引2个字符以上的词
                if keyword not in self.keyword_index:
                    self.keyword_index[keyword] = []
                if faq.id not in self.keyword_index[keyword]:
                    self.keyword_index[keyword].append(faq.id)

    def search(self, query: str, intent: str = None, top_k: int = 3) -> List[FAQSearchResult]:
        """
        搜索FAQ

        Args:
            query: 用户查询
            intent: 用户意图（可选）
            top_k: 返回前K个结果

        Returns:
            List[FAQSearchResult]: 搜索结果
        """
        if not query or not query.strip():
            return []

        query = query.strip()
        query_lower = query.lower()

        results: List[Tuple[FAQItem, float, str]] = []

        # 1. 精确匹配
        for faq in self.faqs.values():
            if faq.question.lower() == query_lower:
                results.append((faq, 1.0, "exact"))

        # 2. 标签匹配 + 问题包含匹配
        query_words = self._extract_keywords(query_lower)
        for faq in self.faqs.values():
            if any(faq.id == r[0].id for r in results):
                continue

            score = 0.0
            match_type = ""

            # 检查标签匹配
            for tag in faq.tags:
                if tag in query_lower:
                    score += 0.3
                    match_type = "tag"
                    break

            # 检查问题是否包含查询词
            for word in query_words:
                if word in faq.question.lower():
                    score += 0.4
                    if not match_type:
                        match_type = "question"
                    break

            # 检查相似度
            similarity = self._calculate_similarity(query_lower, faq.question.lower())
            if similarity > 0.3:
                score = max(score, similarity * 0.8)
                match_type = match_type or "similarity"

            if score > 0.2:
                results.append((faq, min(score, 0.95), match_type))

        # 按得分排序
        results.sort(key=lambda x: x[1], reverse=True)

        # 返回前K个结果
        return [
            FAQSearchResult(faq=r[0], match_score=r[1], match_type=r[2])
            for r in results[:top_k]
        ]

    def _extract_keywords(self, text: str) -> set:
        """提取中文词"""
        return set(re.findall(r'[\u4e00-\u9fa5]{2,}', text.lower()))

    def get_faq_by_id(self, faq_id: str) -> Optional[FAQItem]:
        """根据ID获取FAQ"""
        return self.faqs.get(faq_id)

    def get_faqs_by_category(self, category: str) -> List[FAQItem]:
        """根据分类获取FAQ"""
        return [faq for faq in self.faqs.values() if faq.category == category]

    def _extract_keywords_from_question(self, question: str) -> List[str]:
        """从问题中提取关键词"""
        # 提取2个字符以上的词
        words = re.findall(r'[\u4e00-\u9fa5]{2,}', question.lower())
        # 过滤常见词（保留有意义的词）
        stopwords = {'请问', '如何', '怎样', '怎么', '什么', '哪个', '有没有', '可以', '是否'}
        return [w for w in words if w not in stopwords and len(w) >= 2]

    def _calculate_similarity(self, text1: str, text2: str) -> float:
        """计算两个文本的相似度"""
        # 提取中文词
        words1 = set(re.findall(r'[\u4e00-\u9fa5]{2,}', text1))
        words2 = set(re.findall(r'[\u4e00-\u9fa5]{2,}', text2))

        if not words1 or not words2:
            return 0.0

        # 计算重叠率
        intersection = words1 & words2
        union = words1 | words2

        return len(intersection) / len(union) if union else 0.0


# 全局实例
faq_knowledge_base = FAQKnowledgeBase()
