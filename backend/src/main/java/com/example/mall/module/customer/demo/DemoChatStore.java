package com.example.mall.module.customer.demo;

import com.example.mall.module.customer.demo.DemoChatController.DemoChatMessageRequest;
import com.example.mall.module.customer.demo.DemoChatController.DemoChatMessageResponse;
import com.example.mall.module.customer.demo.DemoChatController.DemoConversationResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DemoChatStore {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, DemoConversation> conversations = new LinkedHashMap<>();
    private final Map<String, List<DemoMessage>> messageMap = new LinkedHashMap<>();

    public DemoChatStore() {
        seedConversation(
            1L,
            "CV202606250001",
            "DY202606250001",
            "Aurora X1 智能手机",
            "DY_SHOP_DEMO",
            "星链数码旗舰店",
            "处理中",
            "AI_SERVING",
            "退货退款",
            List.of(
                new DemoMessage("seed-1", "CONSUMER", "我", "手机屏幕有划痕，可以退货吗？", LocalDateTime.of(2026, 6, 25, 9, 31)),
                new DemoMessage("seed-2", "AI", "AI客服", "可以先上传商品照片和包装照片，我会根据售后规则帮你判断。", LocalDateTime.of(2026, 6, 25, 9, 32)),
                new DemoMessage("seed-3", "CONSUMER", "我", "退款进度在哪里看？", LocalDateTime.of(2026, 6, 25, 9, 34)),
                new DemoMessage("seed-4", "AI", "AI客服", "你可以在订单详情中查看售后进度，也可以告诉我订单号。", LocalDateTime.of(2026, 6, 25, 9, 35))
            )
        );
        seedConversation(
            2L,
            "CV202606250002",
            "DY202606250002",
            "Breeze Pods 无线耳机",
            "DY_SHOP_DEMO",
            "Breeze 声学专营店",
            "未申请",
            "AI_SERVING",
            "物流查询",
            List.of(
                new DemoMessage("seed-5", "CONSUMER", "我", "快递三天没更新了", LocalDateTime.of(2026, 6, 25, 12, 5)),
                new DemoMessage("seed-6", "AI", "AI客服", "当前咨询的是 Breeze 声学专营店订单，耳机类售后可查询退换货和物流进度。", LocalDateTime.of(2026, 6, 25, 12, 6))
            )
        );
        seedConversation(
            3L,
            "CV202606270001",
            "TM202606270001",
            "20商城 青轴机械键盘",
            "20230141",
            "极光外设旗舰店",
            "处理中",
            "AI_SERVING",
            "退货退款",
            List.of(
                new DemoMessage("tm-seed-1", "CONSUMER", "我", "20商城订单的键盘空格键回弹异常，可以退货吗？", LocalDateTime.of(2026, 6, 27, 10, 30)),
                new DemoMessage("tm-seed-2", "AI", "AI客服", "可以。当前订单来自20商城本地数据库，按键故障属于质量问题，请上传商品照片或视频，商家审核通过后进入退货退款流程。", LocalDateTime.of(2026, 6, 27, 10, 31)),
                new DemoMessage("tm-seed-3", "CONSUMER", "我", "这个售后现在处理到哪一步了？", LocalDateTime.of(2026, 6, 27, 10, 34)),
                new DemoMessage("tm-seed-4", "AI", "AI客服", "20商城订单 TM202606270001 当前售后状态为：处理中。", LocalDateTime.of(2026, 6, 27, 10, 35))
            )
        );
        seedConversation(
            4L,
            "CV202606270002",
            "TM202606270002",
            "20商城 城市通勤背包",
            "20230142",
            "黑曜通勤箱包店",
            "未申请",
            "AI_SERVING",
            "售后咨询",
            List.of(
                new DemoMessage("tm-bag-seed-1", "CONSUMER", "我", "这个背包防水吗？如果收到有破损可以售后吗？", LocalDateTime.of(2026, 6, 27, 11, 10)),
                new DemoMessage("tm-bag-seed-2", "AI", "AI客服", "这款背包支持日常防泼水；如收到后存在破损、拉链异常或与描述不符，可在订单详情中申请售后并上传照片凭证。", LocalDateTime.of(2026, 6, 27, 11, 11))
            )
        );
    }

    public synchronized List<DemoConversationResponse> listConversations(List<String> merchantAccounts) {
        return conversations.values().stream()
            .filter(conversation -> merchantAccounts == null || merchantAccounts.isEmpty() || merchantAccounts.contains(conversation.merchantAccountNo()))
            .sorted(Comparator.comparing(DemoConversation::lastMessageAt).reversed())
            .map(this::toConversationResponse)
            .toList();
    }

    public synchronized DemoConversationResponse getConversation(String orderNo) {
        return toConversationResponse(requireConversation(orderNo));
    }

    public synchronized List<DemoChatMessageResponse> listMessages(String orderNo) {
        requireConversation(orderNo);
        return messageMap.getOrDefault(orderNo, List.of()).stream()
            .map(message -> toMessageResponse(orderNo, message))
            .toList();
    }

    public synchronized DemoChatMessageResponse addMessage(String orderNo, DemoChatMessageRequest request) {
        DemoConversation conversation = requireConversation(orderNo);
        String senderType = normalizeSender(request.senderType());
        DemoMessage message = new DemoMessage(
            "msg-" + System.currentTimeMillis(),
            senderType,
            speaker(senderType),
            request.content(),
            LocalDateTime.now()
        );
        addRawMessage(conversation, message);
        if ("CONSUMER".equals(senderType) && "AI_SERVING".equals(conversation.status())) {
            addRawMessage(conversation, buildAiReply(conversation, request.content()));
        }
        return toMessageResponse(orderNo, message);
    }

    public synchronized DemoConversationResponse transferToStaff(String orderNo) {
        DemoConversation current = requireConversation(orderNo);
        DemoConversation updated = new DemoConversation(
            current.id(),
            current.conversationNo(),
            current.orderNo(),
            current.productName(),
            current.merchantAccountNo(),
            current.merchantName(),
            current.afterSaleStatus(),
            "AGENT_SERVING",
            current.aiIntent(),
            current.lastMessage(),
            current.lastMessageAt()
        );
        conversations.put(orderNo, updated);
        addRawMessage(updated, new DemoMessage(
            "transfer-" + System.currentTimeMillis(),
            "STAFF",
            "人工客服",
            "您好，人工客服已接入，请继续描述需要处理的问题。",
            LocalDateTime.now()
        ));
        return toConversationResponse(conversations.get(orderNo));
    }

    private void seedConversation(
        Long id,
        String conversationNo,
        String orderNo,
        String productName,
        String merchantAccountNo,
        String merchantName,
        String afterSaleStatus,
        String status,
        String aiIntent,
        List<DemoMessage> messages
    ) {
        DemoMessage last = messages.get(messages.size() - 1);
        conversations.put(orderNo, new DemoConversation(id, conversationNo, orderNo, productName, merchantAccountNo, merchantName, afterSaleStatus, status, aiIntent, last.content(), last.createdAt()));
        messageMap.put(orderNo, new ArrayList<>(messages));
    }

    private void addRawMessage(DemoConversation conversation, DemoMessage message) {
        messageMap.computeIfAbsent(conversation.orderNo(), key -> new ArrayList<>()).add(message);
        conversations.put(conversation.orderNo(), new DemoConversation(
            conversation.id(),
            conversation.conversationNo(),
            conversation.orderNo(),
            conversation.productName(),
            conversation.merchantAccountNo(),
            conversation.merchantName(),
            conversation.afterSaleStatus(),
            conversation.status(),
            conversation.aiIntent(),
            message.content(),
            message.createdAt()
        ));
    }

    private DemoMessage buildAiReply(DemoConversation conversation, String content) {
        String reply = "我已收到你的问题，会结合订单和售后规则为你查询。";
        if (content.contains("退货政策") || content.contains("七天无理由")) {
            reply = "当前退货政策为：签收 7 天内，商品不影响二次销售且包装配件完整时可申请退货；如商品存在质量问题，请上传商品照片和包装照片，商家审核通过后进入退货退款流程。";
        } else if (content.contains("退款") || content.contains("进度")) {
            reply = "你可以在订单详情中查看售后进度。当前订单 " + conversation.orderNo() + " 的售后状态为：" + conversation.afterSaleStatus() + "。";
        } else if (content.contains("订单号")) {
            reply = "我已识别到当前咨询订单：" + conversation.orderNo() + "，商品为：" + conversation.productName() + "。";
        }
        return new DemoMessage("ai-" + System.currentTimeMillis(), "AI", "AI客服", reply, LocalDateTime.now());
    }

    private DemoConversation requireConversation(String orderNo) {
        DemoConversation conversation = conversations.get(orderNo);
        if (conversation == null) {
            throw new IllegalArgumentException("Demo conversation not found: " + orderNo);
        }
        return conversation;
    }

    private DemoConversationResponse toConversationResponse(DemoConversation conversation) {
        return new DemoConversationResponse(
            conversation.id(),
            conversation.conversationNo(),
            conversation.orderNo(),
            conversation.productName(),
            conversation.merchantAccountNo(),
            conversation.merchantName(),
            conversation.afterSaleStatus(),
            conversation.status(),
            conversation.aiIntent(),
            conversation.lastMessage(),
            formatTime(conversation.lastMessageAt())
        );
    }

    private DemoChatMessageResponse toMessageResponse(String orderNo, DemoMessage message) {
        return new DemoChatMessageResponse(message.id(), orderNo, message.senderType(), message.speaker(), message.content(), formatTime(message.createdAt()));
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private String normalizeSender(String senderType) {
        String normalized = senderType.toUpperCase(Locale.ROOT);
        if ("USER".equals(normalized)) {
            return "CONSUMER";
        }
        if ("AGENT".equals(normalized) || "CUSTOMER_SERVICE".equals(normalized)) {
            return "STAFF";
        }
        return normalized;
    }

    private String speaker(String senderType) {
        return switch (senderType) {
            case "CONSUMER" -> "我";
            case "STAFF" -> "人工客服";
            case "AI" -> "AI客服";
            default -> "系统";
        };
    }

    private record DemoConversation(
        Long id,
        String conversationNo,
        String orderNo,
        String productName,
        String merchantAccountNo,
        String merchantName,
        String afterSaleStatus,
        String status,
        String aiIntent,
        String lastMessage,
        LocalDateTime lastMessageAt
    ) {
    }

    private record DemoMessage(String id, String senderType, String speaker, String content, LocalDateTime createdAt) {
    }
}
