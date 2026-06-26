package com.example.mall.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record AfterSaleRuleRequest(
    @NotBlank(message = "规则名称不能为空")
    String ruleName,
    @NotBlank(message = "规则类型不能为空")
    String ruleType,
    String conditionsJson,
    String actionJson,
    String content,
    Boolean enabled
) {
}
