package com.example.mall.module.knowledge.dto;

import java.time.LocalDateTime;

public record KnowledgeArticleResponse(
    Long id,
    Long merchantId,
    String title,
    String content,
    String category,
    String tags,
    String status,
    Long createdBy,
    LocalDateTime createdAt
) {
}
