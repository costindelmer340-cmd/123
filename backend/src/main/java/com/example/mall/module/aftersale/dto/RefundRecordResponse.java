package com.example.mall.module.aftersale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundRecordResponse(
    Long id,
    String refundNo,
    String platformCode,
    String externalRefundNo,
    BigDecimal refundAmount,
    String refundStatus,
    String reason,
    LocalDateTime refundedAt
) {
}
