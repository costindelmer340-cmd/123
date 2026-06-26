package com.example.mall.module.ai.dto;

public record IntentResponse(String intent, String category, Double confidence, String summary) {
}
