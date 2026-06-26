package com.example.mall.module.review.service;

import com.example.mall.common.response.PageResponse;
import com.example.mall.module.review.dto.ReviewAnalysisResponse;
import com.example.mall.module.review.dto.ReviewDetailResponse;
import com.example.mall.module.review.dto.ReviewSummaryResponse;

public interface ReviewService {

    PageResponse<ReviewSummaryResponse> pageMerchantReviews(long pageNum, long pageSize, String sentiment, String keyword);

    ReviewDetailResponse reviewDetail(Long reviewId);

    ReviewAnalysisResponse analyzeReview(Long reviewId);
}
