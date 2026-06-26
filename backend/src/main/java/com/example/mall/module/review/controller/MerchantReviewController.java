package com.example.mall.module.review.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.review.dto.ReviewAnalysisResponse;
import com.example.mall.module.review.dto.ReviewDetailResponse;
import com.example.mall.module.review.dto.ReviewSummaryResponse;
import com.example.mall.module.review.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/reviews")
public class MerchantReviewController {

    private final ReviewService reviewService;

    public MerchantReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewSummaryResponse>> page(
        @RequestParam(defaultValue = "1") long pageNum,
        @RequestParam(defaultValue = "10") long pageSize,
        @RequestParam(required = false) String sentiment,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(reviewService.pageMerchantReviews(pageNum, pageSize, sentiment, keyword), traceId());
    }

    @GetMapping("/{reviewId}")
    public ApiResponse<ReviewDetailResponse> detail(@PathVariable Long reviewId) {
        return ApiResponse.success(reviewService.reviewDetail(reviewId), traceId());
    }

    @PostMapping("/{reviewId}/analyze")
    public ApiResponse<ReviewAnalysisResponse> analyze(@PathVariable Long reviewId) {
        return ApiResponse.success(reviewService.analyzeReview(reviewId), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
