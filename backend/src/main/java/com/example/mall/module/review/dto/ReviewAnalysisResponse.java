package com.example.mall.module.review.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewAnalysisResponse(
    Long id,
    String sentiment,
    BigDecimal sentimentScore,
    String topics,
    String keywords,
    String riskLevel,
    String summary,
    LocalDateTime analyzedAt
) {
}
