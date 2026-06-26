package com.example.mall.module.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
    Long id,
    Long shopBindingId,
    String platformCode,
    String externalOrderNo,
    String buyerMaskedName,
    String buyerMaskedPhone,
    String orderStatus,
    String payStatus,
    String logisticsStatus,
    String afterSaleStatus,
    BigDecimal totalAmount,
    BigDecimal payableAmount,
    LocalDateTime paidAt,
    LocalDateTime orderedAt,
    LocalDateTime completedAt,
    LocalDateTime lastSyncedAt
) {
}
