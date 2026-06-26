package com.example.mall.module.aftersale.dto;

import java.time.LocalDateTime;

public record ExternalAfterSaleMappingResponse(
    Long id,
    String platformCode,
    String externalAfterSaleNo,
    String externalRefundNo,
    String externalStatus,
    LocalDateTime lastSyncedAt
) {
}
