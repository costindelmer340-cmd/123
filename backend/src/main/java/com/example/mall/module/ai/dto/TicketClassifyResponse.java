package com.example.mall.module.ai.dto;

public record TicketClassifyResponse(String ticketType, String priority, String category, Double confidence, Integer dueHours) {
}
