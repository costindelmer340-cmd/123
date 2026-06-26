package com.example.mall.module.ticket.dto;

import java.time.LocalDateTime;

public record TicketRecordResponse(
    Long id,
    Long ticketId,
    Long operatorId,
    String actionType,
    String fromStatus,
    String toStatus,
    String content,
    LocalDateTime createdAt
) {
}
