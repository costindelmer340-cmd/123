package com.example.mall.module.ticket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketSummaryResponse(
    Long id,
    String ticketNo,
    Long afterSaleId,
    Long conversationId,
    Long externalOrderId,
    Long userId,
    Long merchantId,
    Long assignedStaffId,
    String ticketType,
    String title,
    String status,
    String priority,
    String aiCategory,
    BigDecimal aiConfidence,
    LocalDateTime dueAt,
    LocalDateTime closedAt
) {
}
