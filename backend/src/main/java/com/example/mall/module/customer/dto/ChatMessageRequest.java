package com.example.mall.module.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageRequest(
    @NotNull(message = "会话ID不能为空")
    Long conversationId,

    @NotBlank(message = "消息内容不能为空")
    String content
) {
}
