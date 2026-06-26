package com.example.mall.module.ai.dto;

import java.util.List;

public record TopicResponse(List<String> topics, List<String> keywords, String summary) {
}
