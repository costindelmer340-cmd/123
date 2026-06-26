package com.example.mall.module.customer.dto;

import jakarta.validation.constraints.NotNull;

public record StartConversationRequest(
    @NotNull(message = "商家ID不能为空")
    Long merchantId,

    Long externalOrderId
) {
}
