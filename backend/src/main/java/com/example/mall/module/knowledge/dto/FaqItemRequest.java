package com.example.mall.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record FaqItemRequest(
    @NotBlank(message = "问题不能为空")
    String question,
    @NotBlank(message = "答案不能为空")
    String answer,
    @NotBlank(message = "分类不能为空")
    String category,
    Integer priority,
    Boolean enabled
) {
}
