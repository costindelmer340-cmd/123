package com.example.mall.module.review.dto;

import java.time.LocalDateTime;

public record ReviewSummaryResponse(
    Long id,
    Long externalOrderId,
    Long externalOrderItemId,
    String platformCode,
    String externalReviewId,
    String reviewSource,
    Integer productScore,
    Integer logisticsScore,
    Integer serviceScore,
    String content,
    Boolean anonymous,
    String status,
    LocalDateTime reviewedAt
) {
}
