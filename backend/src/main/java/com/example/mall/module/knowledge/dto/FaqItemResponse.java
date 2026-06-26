package com.example.mall.module.knowledge.dto;

import java.time.LocalDateTime;

public record FaqItemResponse(
    Long id,
    Long merchantId,
    String question,
    String answer,
    String category,
    Integer priority,
    Boolean enabled,
    Long createdBy,
    LocalDateTime createdAt
) {
}
