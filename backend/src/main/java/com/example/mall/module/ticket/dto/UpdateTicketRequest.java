package com.example.mall.module.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTicketRequest(
    @NotBlank(message = "工单状态不能为空")
    String status,

    String content
) {
}
