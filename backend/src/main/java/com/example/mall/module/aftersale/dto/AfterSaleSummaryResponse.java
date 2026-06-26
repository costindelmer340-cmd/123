package com.example.mall.module.aftersale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AfterSaleSummaryResponse(
    Long id,
    String afterSaleNo,
    Long externalOrderId,
    Long externalOrderItemId,
    Long userId,
    Long merchantId,
    String afterSaleType,
    String reasonType,
    BigDecimal requestedAmount,
    String status,
    String priority,
    String aiCategory,
    LocalDateTime createdAt
) {
}
