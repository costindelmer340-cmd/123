package com.example.mall.module.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    Long conversationId,
    Long senderId,
    String senderType,
    String messageType,
    String content,
    String mediaUrl,
    Boolean aiGenerated,
    BigDecimal aiConfidence,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {
}
