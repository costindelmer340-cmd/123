package com.example.mall.module.knowledge.dto;

import java.time.LocalDateTime;

public record AfterSaleRuleResponse(
    Long id,
    Long merchantId,
    String ruleName,
    String ruleType,
    String conditionsJson,
    String actionJson,
    String content,
    Boolean enabled,
    Long createdBy,
    LocalDateTime createdAt
) {
}
