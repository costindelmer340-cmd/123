package com.example.mall.module.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    String externalItemId,
    String externalProductId,
    String productName,
    String skuName,
    String productImageUrl,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal totalAmount,
    String afterSaleStatus
) {
}
