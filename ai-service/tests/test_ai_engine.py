"""
AI引擎 - 单元测试
"""
import sys
import os

# 获取ai-engine目录的绝对路径
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
sys.path.insert(0, parent_dir)

from modules.intent_recognizer import intent_recognizer
from modules.sentiment_analyzer import sentiment_analyzer
from modules.topic_classifier import topic_classifier
from modules.faq_knowledge_base import faq_knowledge_base
from modules.conversation_manager import conversation_manager
from modules.rag_generator import rag_generator, GenerationContext
from modules.rule_engine import rule_engine


def test_intent_recognition():
    """测试意图识别"""
    print("=" * 50)
    print("测试意图识别")

    test_cases = [
        "我的快递怎么还没到，都等了好几天了",
        "商品坏了，想退货",
        "你好，请问有什么可以帮助您的",
        "太差了，服务态度恶劣，我要投诉",
        "发票怎么开",
    ]

    for query in test_cases:
        result = intent_recognizer.recognize(query)
        print(f"\nQuery: {query}")
        print(f"Intent: {result.primary_intent.value}")
        print(f"Confidence: {result.confidence:.2f}")
        print(f"Keywords: {result.keywords}")
        print(f"Needs Escalation: {result.requires_escalation}")


def test_sentiment_analysis():
    """测试情感分析"""
    print("\n" + "=" * 50)
    print("测试情感分析")

    test_cases = [
        "非常满意，商品质量很好，物流也快",
        "太差了，等了半个月还没到货",
        "还行吧，一般般",
        "垃圾商品，再也不会买了",
        "谢谢你的帮助，很专业",
    ]

    for text in test_cases:
        result = sentiment_analyzer.analyze(text)
        print(f"\nText: {text}")
        print(f"Sentiment: {result.sentiment.value}")
        print(f"Score: {result.sentiment_score:.2f}")
        print(f"Risk Level: {result.risk_level.value}")
        print(f"Risk Keywords: {result.risk_keywords}")


def test_topic_classification():
    """测试主题分类"""
    print("\n" + "=" * 50)
    print("测试主题分类")

    test_cases = [
        "快递怎么还没到，物流太慢了",
        "商品有质量问题，申请退货",
        "发票怎么开具，需要什么材料",
        "客服态度太差了，要投诉",
        "这个价格能不能便宜点",
    ]

    for text in test_cases:
        result = topic_classifier.classify(text)
        print(f"\nText: {text}")
        print(f"Primary Topic: {result.primary_topic}")
        print(f"Topics: {result.topics}")
        print(f"Tags: {result.tags}")


def test_faq_search():
    """测试FAQ搜索"""
    print("\n" + "=" * 50)
    print("测试FAQ搜索")

    test_cases = [
        "物流多久到",
        "退货怎么操作",
        "发票问题",
    ]

    for query in test_cases:
        results = faq_knowledge_base.search(query, top_k=3)
        print(f"\nQuery: {query}")
        print(f"Results: {len(results)}")
        for r in results:
            print(f"  - {r.faq.question} (score: {r.match_score:.2f})")


def test_conversation():
    """测试会话管理"""
    print("\n" + "=" * 50)
    print("测试会话管理")

    # 创建会话
    conv_id = conversation_manager.generate_conversation_id(1001, 50001)
    context = conversation_manager.create_conversation(
        conversation_id=conv_id,
        merchant_id=1001,
        user_id=50001
    )

    # 添加消息
    conversation_manager.add_user_message(conv_id, "你好，我想问一下物流问题")
    conversation_manager.add_assistant_message(conv_id, "您好，请问有什么可以帮助您的？")

    # 获取历史
    history = conversation_manager.get_conversation_history(conv_id)
    print(f"\nConversation ID: {conv_id}")
    print(f"Messages: {len(history)}")
    for msg in history:
        print(f"  [{msg.role.value}] {msg.content[:30]}...")


def test_rag_response():
    """测试RAG回复生成"""
    print("\n" + "=" * 50)
    print("测试RAG回复生成")

    test_cases = [
        ("物流太慢了，我都等了10天了", "物流延迟", "negative"),
        ("你好啊", "问候", "positive"),
        ("商品坏了", "商品质量", "negative"),
    ]

    for query, intent, sentiment in test_cases:
        context = GenerationContext(
            query=query,
            intent=intent,
            sentiment=sentiment,
            topics=[intent],
            knowledge_items=[],
            conversation_history=[],
            metadata={}
        )

        response = rag_generator.generate(context)
        print(f"\nQuery: {query}")
        print(f"Intent: {intent}")
        print(f"Response: {response.content[:50]}...")
        print(f"Strategy: {response.strategy.value}")
        print(f"Escalate: {response.escalation}")


def test_rule_engine():
    """测试规则引擎"""
    print("\n" + "=" * 50)
    print("测试规则引擎")

    print("\n--- 测试售后分类规则 ---")
    classify_cases = [
        "商品坏了，想退货退款",
        "尺码不合适，想换货",
        "没收到货，申请退款",
        "手机充不了电，需要维修",
        "少发了一件，要求补发",
        "耽误时间了，要赔偿",
        "你好",
    ]
    for text in classify_cases:
        result = rule_engine.execute("after_sale_classification", {"text": text})
        details = result["rule_results"][0]["details"]
        print(f"\nText: {text}")
        print(f"  Classification: {details['classification']}")
        print(f"  Confidence: {details['confidence']}")
        print(f"  Matched Keywords: {details['matched_keywords']}")

    print("\n--- 测试质检规则 ---")
    inspect_cases = [
        {"order_id": "DD20240101", "user_id": 1001, "after_sale_type": "退货退款", "reason": "商品质量有问题，屏幕出现亮点"},
        {"reason": "坏了"},
        {"order_id": "DD20240102", "after_sale_type": "维修", "reason": "无法开机"},
    ]
    for data in inspect_cases:
        result = rule_engine.execute("quality_inspection", data)
        details = result["rule_results"][0]["details"]
        print(f"\nData: {data}")
        print(f"  Result: {result['overall_result']}")
        print(f"  Quality Level: {details['quality_level']}")
        print(f"  Issues: {details['issues']}")
        print(f"  Warnings: {details['warnings']}")

    print("\n--- 测试转人工规则 ---")
    escalate_cases = [
        {"text": "我要投诉，你们是骗子", "intent": "投诉", "sentiment": "negative", "sentiment_score": 0.1},
        {"text": "我的快递怎么还没到", "intent": "物流延迟", "sentiment": "neutral", "sentiment_score": 0.5},
        {"text": "转人工", "intent": "转人工"},
        {"text": "订单金额20000元有问题", "order_amount": 20000},
    ]
    for data in escalate_cases:
        result = rule_engine.execute("escalation", data)
        details = result["rule_results"][0]["details"]
        print(f"\nText: {data.get('text', '')}")
        print(f"  Result: {result['overall_result']}")
        print(f"  Escalate: {details['escalate']}")
        print(f"  Triggers: {details['triggers']}")
        print(f"  Priority: {details['priority']}")

    print("\n--- 测试评价分析规则 ---")
    review_cases = [
        {"text": "商品质量很好，物流也快，非常满意", "rating": 5},
        {"text": "物流太慢了，等了半个月才到，包装也破损了", "rating": 2},
        {"text": "客服态度很差，问了半天不理人", "rating": 1},
        {"text": "价格有点贵，但质量还可以", "rating": 3},
    ]
    for data in review_cases:
        result = rule_engine.execute("review_analysis", data)
        details = result["rule_results"][0]["details"]
        print(f"\nText: {data['text']}")
        print(f"  Sentiment: {details['sentiment']}")
        print(f"  Rating: {details['rating']}")
        print(f"  Issues: {details['issues']}")
        print(f"  Positive: {details['positive_points']}")
        print(f"  Negative: {details['negative_points']}")
        print(f"  Summary: {details['summary']}")


def run_all_tests():
    """运行所有测试"""
    print("=" * 60)
    print("开始AI引擎单元测试")
    print("=" * 60)

    test_intent_recognition()
    test_sentiment_analysis()
    test_topic_classification()
    test_faq_search()
    test_conversation()
    test_rag_response()
    test_rule_engine()

    print("\n" + "=" * 60)
    print("所有测试完成！")
    print("=" * 60)


if __name__ == "__main__":
    run_all_tests()
