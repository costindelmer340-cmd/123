"""
AI引擎 - 主题归类模块
对用户反馈进行主题分类
"""
import re
from typing import List, Dict, Tuple
from collections import Counter
from dataclasses import dataclass


@dataclass
class TopicResult:
    """主题归类结果"""
    topics: List[Tuple[str, float]]  # [(主题, 置信度), ...]
    primary_topic: str
    tags: List[str]
    keywords: List[str]


class TopicClassifier:
    """主题分类器"""

    def __init__(self):
        # 主题定义及其关键词
        self.topic_definitions = {
            '物流配送': {
                'keywords': ['物流', '快递', '配送', '发货', '送货', '运输', '到货', '收货',
                           '延迟', '慢', '停', '丢件', '快递员', '小哥', '配送员', '签收',
                           '未到', '未收到', '几天', '多久', '迟迟', '催促', '揽收'],
                'weight': 1.0
            },
            '商品质量': {
                'keywords': ['质量', '品质', '损坏', '破损', '坏了', '碎', '裂', '故障',
                           '异常', '不能用', '功能', '描述不符', '图片', '假货', '仿冒',
                           '山寨', '异味', '味道', '材质', '瑕疵', '缺陷', '有问题'],
                'weight': 1.0
            },
            '退货退款': {
                'keywords': ['退货', '退款', '退', '退还', '取消订单', '后悔', '不想要',
                           '换货', '更换', '换', '尺寸', '大小', '颜色', '不合适', '不喜欢',
                           '退钱', '退到哪', '多久到账', '退款进度', '审核'],
                'weight': 0.9
            },
            '服务态度': {
                'keywords': ['客服', '服务', '态度', '不理', '不回', '不回消息', '敷衍',
                           '推诿', '拖延', '恶劣', '差', '不好', '投诉', '举报', '不耐烦',
                           '口气', '语气', '凶', '骂', '吼'],
                'weight': 1.0
            },
            '价格争议': {
                'keywords': ['价格', '价钱', '费用', '贵', '高', '便宜', '优惠', '降价',
                           '差价', '优惠', '优惠券', '红包', '抵扣', '折扣', '打折',
                           '能便宜', '便宜点', '优惠点'],
                'weight': 0.8
            },
            '发票问题': {
                'keywords': ['发票', '收据', '普票', '专票', '增票', '税点', '税金',
                           '报销', '报账', '开票', '补开', '重开', '更换发票'],
                'weight': 0.9
            },
            '商品信息': {
                'keywords': ['规格', '参数', '型号', '版本', '款式', '颜色', '尺寸',
                           '容量', '材质', '面料', '成分', '产地', '品牌', '说明书',
                           '用法', '使用方法', '怎么用', '功能', '效果'],
                'weight': 0.7
            },
            '售后政策': {
                'keywords': ['保修', '质保', '维修', '三包', '售后', '保障', '承诺',
                           '期限', '有效期', '退换', '条件', '要求', '规则', '规定'],
                'weight': 0.8
            },
            '支付问题': {
                'keywords': ['支付', '付款', '扣款', '退款', '转账', '银行卡', '微信',
                           '支付宝', '到账', '支付失败', '无法支付', '支付方式'],
                'weight': 0.9
            },
            '账户问题': {
                'keywords': ['账号', '账户', '登录', '密码', '手机号', '邮箱', '认证',
                           '实名', '绑定', '解绑', '注销', '封号', '异常'],
                'weight': 0.7
            },
        }

        # 停用词（不计入主题判断）
        self.stopwords = {
            '这个', '那个', '请问', '你好', '您好', '我想', '我要', '帮我',
            '可以', '能够', '麻烦', '请问', '一下', '帮忙', '请问', '到底',
            '到底', '为什么', '怎么', '如何', '哪里', '什么'
        }

    def classify(self, text: str, top_n: int = 3) -> TopicResult:
        """
        对文本进行主题分类

        Args:
            text: 待分类的文本
            top_n: 返回前N个主题

        Returns:
            TopicResult: 主题分类结果
        """
        if not text or not text.strip():
            return TopicResult(
                topics=[],
                primary_topic='未知',
                tags=[],
                keywords=[]
            )

        text = text.strip().lower()

        # 提取关键词
        keywords = self._extract_keywords(text)

        # 计算每个主题的得分
        topic_scores = {}
        for topic_name, topic_def in self.topic_definitions.items():
            score = self._calculate_topic_score(text, keywords, topic_def)
            if score > 0:
                topic_scores[topic_name] = score

        # 按得分排序
        sorted_topics = sorted(
            topic_scores.items(),
            key=lambda x: x[1],
            reverse=True
        )

        # 取前N个主题
        top_topics = sorted_topics[:top_n]

        # 计算归一化的置信度
        if top_topics:
            max_score = top_topics[0][1]
            normalized_topics = [
                (topic, score / max_score if max_score > 0 else 0)
                for topic, score in top_topics
            ]
        else:
            normalized_topics = []

        # 生成标签
        tags = self._generate_tags(normalized_topics)

        return TopicResult(
            topics=normalized_topics,
            primary_topic=normalized_topics[0][0] if normalized_topics else '其他',
            tags=tags,
            keywords=keywords
        )

    def _extract_keywords(self, text: str) -> List[str]:
        """提取关键词"""
        # 提取中文词组
        chinese_words = re.findall(r'[\u4e00-\u9fa5]{2,}', text)

        # 过滤停用词
        keywords = [w for w in chinese_words if w not in self.stopwords]

        # 统计词频
        word_freq = Counter(keywords)

        # 返回出现次数最多的词
        return [word for word, _ in word_freq.most_common(10)]

    def _calculate_topic_score(self, text: str, keywords: List[str],
                               topic_def: Dict) -> float:
        """计算文本属于某个主题的得分"""
        topic_keywords = topic_def['keywords']
        weight = topic_def['weight']

        score = 0.0

        # 检查关键词匹配
        for kw in topic_keywords:
            if kw in text:
                # 根据关键词长度给予不同权重
                if len(kw) >= 3:
                    score += 2.0 * weight
                elif len(kw) == 2:
                    score += 1.5 * weight
                else:
                    score += 1.0 * weight

        # 检查提取的关键词
        for kw in keywords:
            if kw in topic_keywords:
                score += 1.0 * weight

        # 考虑关键词密度（匹配数/总词数）
        matched_keywords = sum(1 for kw in topic_keywords if kw in text)
        density = matched_keywords / len(topic_keywords) if topic_keywords else 0
        score *= (1 + density)

        return score

    def _generate_tags(self, topics: List[Tuple[str, float]]) -> List[str]:
        """生成标签"""
        tags = []

        # 基于主题生成标签
        for topic, confidence in topics:
            if confidence >= 0.8:
                tags.append(topic)
            elif confidence >= 0.6:
                # 添加细分标签
                if topic == '物流配送':
                    tags.append('配送问题')
                elif topic == '商品质量':
                    tags.append('质量问题')
                elif topic == '服务态度':
                    tags.append('服务投诉')

        return tags[:5]  # 最多5个标签


# 全局实例
topic_classifier = TopicClassifier()
