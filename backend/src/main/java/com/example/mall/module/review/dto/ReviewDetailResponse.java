package com.example.mall.module.review.dto;

import java.util.List;

public record ReviewDetailResponse(
    ReviewSummaryResponse review,
    List<ReviewAppendResponse> appends,
    ReviewAnalysisResponse analysis
) {
}
