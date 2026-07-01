package com.example.mall.module.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiReplyRequest(
    @NotBlank(message = "文本内容不能为空")
    String text,
    Long merchantId,
    String businessType,
    Long businessId,
    String orderStatus,
    String afterSaleStatus,
    String userTone,
    String sessionId
) {
}
