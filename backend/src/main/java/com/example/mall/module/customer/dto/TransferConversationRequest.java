package com.example.mall.module.customer.dto;

import jakarta.validation.constraints.NotNull;

public record TransferConversationRequest(
    @NotNull(message = "客服ID不能为空")
    Long staffId
) {
}
