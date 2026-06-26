package com.example.mall.module.order.dto;

import java.time.LocalDateTime;

public record LogisticsSnapshotResponse(
    Long id,
    String logisticsCompany,
    String trackingNo,
    String logisticsStatus,
    LocalDateTime shippedAt,
    LocalDateTime receivedAt,
    String trackingDetail,
    LocalDateTime lastSyncedAt
) {
}
