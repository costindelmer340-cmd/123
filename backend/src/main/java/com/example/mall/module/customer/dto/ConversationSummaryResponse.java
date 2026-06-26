package com.example.mall.module.customer.dto;

import java.time.LocalDateTime;

public record ConversationSummaryResponse(
    Long id,
    String conversationNo,
    Long userId,
    Long merchantId,
    Long externalOrderId,
    Long assignedStaffId,
    String source,
    String status,
    String lastMessage,
    LocalDateTime lastMessageAt,
    String aiIntent,
    String aiSummary
) {
}
