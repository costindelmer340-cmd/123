package com.example.mall.module.order.dto;

import java.util.List;

public record OrderDetailResponse(
    OrderSummaryResponse order,
    List<OrderItemResponse> items,
    List<PaymentSnapshotResponse> payments,
    List<LogisticsSnapshotResponse> logistics
) {
}
