package com.example.mall.module.ai.dto;

import java.util.List;

public record ReviewAnalysisResponse(
    String sentiment,
    Double sentimentScore,
    List<String> topics,
    List<String> keywords,
    String riskLevel,
    String summary,
    String suggestion
) {
}