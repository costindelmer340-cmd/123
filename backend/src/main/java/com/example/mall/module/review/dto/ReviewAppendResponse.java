package com.example.mall.module.review.dto;

import java.time.LocalDateTime;

public record ReviewAppendResponse(Long id, String content, String imageUrls, LocalDateTime appendedAt) {
}
