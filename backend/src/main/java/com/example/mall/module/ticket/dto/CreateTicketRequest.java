package com.example.mall.module.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
    @NotNull(message = "会话ID不能为空")
    Long conversationId,

    @NotBlank(message = "工单类型不能为空")
    String ticketType,

    @NotBlank(message = "标题不能为空")
    String title,

    String description,

    String priority
) {
}
