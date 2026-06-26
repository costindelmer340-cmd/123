package com.example.mall.module.platform.dto;

import java.time.LocalDateTime;

public record ShopBindingResponse(
    Long id,
    Long merchantId,
    Long platformId,
    String platformCode,
    String externalShopId,
    String shopName,
    String sellerNick,
    String authStatus,
    LocalDateTime lastSyncedAt,
    LocalDateTime createdAt
) {
}
