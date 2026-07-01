package com.example.mall.module.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.auth.SecurityUtils;
import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.ai.dto.AiTextRequest;
import com.example.mall.module.ai.service.AiService;
import com.example.mall.module.review.dto.ReviewAnalysisResponse;
import com.example.mall.module.review.dto.ReviewAppendResponse;
import com.example.mall.module.review.dto.ReviewDetailResponse;
import com.example.mall.module.review.dto.ReviewSummaryResponse;
import com.example.mall.module.review.entity.Review;
import com.example.mall.module.review.entity.ReviewAnalysis;
import com.example.mall.module.review.entity.ReviewAppend;
import com.example.mall.module.review.mapper.ReviewAnalysisMapper;
import com.example.mall.module.review.mapper.ReviewAppendMapper;
import com.example.mall.module.review.mapper.ReviewMapper;
import com.example.mall.module.review.service.ReviewService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewAppendMapper reviewAppendMapper;
    private final ReviewAnalysisMapper reviewAnalysisMapper;
    private final AiService aiService;

    public ReviewServiceImpl(
        ReviewMapper reviewMapper,
        ReviewAppendMapper reviewAppendMapper,
        ReviewAnalysisMapper reviewAnalysisMapper,
        AiService aiService
    ) {
        this.reviewMapper = reviewMapper;
        this.reviewAppendMapper = reviewAppendMapper;
        this.reviewAnalysisMapper = reviewAnalysisMapper;
        this.aiService = aiService;
    }

    @Override
    public PageResponse<ReviewSummaryResponse> pageMerchantReviews(long pageNum, long pageSize, String sentiment, String keyword) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Page<Review> page = reviewMapper.selectPage(
            new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
            new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchantId)
                .like(keyword != null && !keyword.isBlank(), Review::getContent, keyword)
                .orderByDesc(Review::getReviewedAt)
                .orderByDesc(Review::getId)
        );
        List<ReviewSummaryResponse> records = page.getRecords().stream().map(this::toSummaryResponse).toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    public ReviewDetailResponse reviewDetail(Long reviewId) {
        Review review = requireMerchantReview(reviewId);
        List<ReviewAppendResponse> appends = reviewAppendMapper.selectList(
            new LambdaQueryWrapper<ReviewAppend>()
                .eq(ReviewAppend::getReviewId, reviewId)
                .orderByAsc(ReviewAppend::getAppendedAt)
        ).stream().map(this::toAppendResponse).toList();
        ReviewAnalysis analysis = reviewAnalysisMapper.selectOne(
            new LambdaQueryWrapper<ReviewAnalysis>()
                .eq(ReviewAnalysis::getReviewId, reviewId)
                .last("limit 1")
        );
        return new ReviewDetailResponse(toSummaryResponse(review), appends, analysis == null ? null : toAnalysisResponse(analysis));
    }

    @Override
    @Transactional
    public com.example.mall.module.review.dto.ReviewAnalysisResponse analyzeReview(Long reviewId) {
        Review review = requireMerchantReview(reviewId);
        String content = review.getContent() == null ? "" : review.getContent();
        AiTextRequest aiRequest = new AiTextRequest(content, review.getMerchantId(), "REVIEW", review.getId());
        com.example.mall.module.ai.dto.ReviewAnalysisResponse aiResponse = aiService.analyzeReview(aiRequest);

        ReviewAnalysis analysis = reviewAnalysisMapper.selectOne(
            new LambdaQueryWrapper<ReviewAnalysis>()
                .eq(ReviewAnalysis::getReviewId, review.getId())
                .last("limit 1")
        );
        if (analysis == null) {
            analysis = new ReviewAnalysis();
            analysis.setReviewId(review.getId());
        }
        analysis.setSentiment(aiResponse.sentiment());
        analysis.setSentimentScore(BigDecimal.valueOf(aiResponse.sentimentScore()));
        analysis.setTopics(toJsonArray(aiResponse.topics()));
        analysis.setKeywords(toJsonArray(aiResponse.keywords()));
        analysis.setRiskLevel(aiResponse.riskLevel());
        analysis.setSummary(aiResponse.summary());
        analysis.setAnalyzedAt(LocalDateTime.now());
        if (analysis.getId() == null) {
            reviewAnalysisMapper.insert(analysis);
        } else {
            reviewAnalysisMapper.updateById(analysis);
        }
        return toAnalysisResponse(analysis);
    }

    private Review requireMerchantReview(Long reviewId) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Review review = reviewMapper.selectById(reviewId);
        if (review == null || !merchantId.equals(review.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "Review not found");
        }
        return review;
    }

    private String toJsonArray(List<String> values) {
        return "[\"" + String.join("\",\"", values.stream().map(value -> value.replace("\"", "\\\"")).toList()) + "\"]";
    }

    private ReviewSummaryResponse toSummaryResponse(Review review) {
        return new ReviewSummaryResponse(
            review.getId(),
            review.getExternalOrderId(),
            review.getExternalOrderItemId(),
            review.getPlatformCode(),
            review.getExternalReviewId(),
            review.getReviewSource(),
            review.getProductScore(),
            review.getLogisticsScore(),
            review.getServiceScore(),
            review.getContent(),
            review.getAnonymous() != null && review.getAnonymous() == 1,
            review.getStatus(),
            review.getReviewedAt()
        );
    }

    private ReviewAppendResponse toAppendResponse(ReviewAppend append) {
        return new ReviewAppendResponse(append.getId(), append.getContent(), append.getImageUrls(), append.getAppendedAt());
    }

    private ReviewAnalysisResponse toAnalysisResponse(ReviewAnalysis analysis) {
        return new ReviewAnalysisResponse(
            analysis.getId(),
            analysis.getSentiment(),
            analysis.getSentimentScore(),
            analysis.getTopics(),
            analysis.getKeywords(),
            analysis.getRiskLevel(),
            analysis.getSummary(),
            analysis.getAnalyzedAt()
        );
    }
}
