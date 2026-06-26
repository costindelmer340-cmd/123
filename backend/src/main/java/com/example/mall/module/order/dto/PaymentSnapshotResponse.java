package com.example.mall.module.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSnapshotResponse(
    Long id,
    String externalPaymentNo,
    String payChannel,
    String payStatus,
    BigDecimal paidAmount,
    LocalDateTime paidAt,
    LocalDateTime lastSyncedAt
) {
}
