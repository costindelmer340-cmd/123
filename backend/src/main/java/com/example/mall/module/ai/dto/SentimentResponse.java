package com.example.mall.module.ai.dto;

public record SentimentResponse(String sentiment, Double score, String riskLevel, String summary) {
}
