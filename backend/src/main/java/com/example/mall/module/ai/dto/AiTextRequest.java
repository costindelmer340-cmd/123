package com.example.mall.module.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiTextRequest(
    @NotBlank(message = "文本内容不能为空")
    String text,
    Long merchantId,
    String businessType,
    Long businessId
) {
}
