package com.example.mall.module.order.service;

import com.example.mall.common.response.PageResponse;
import com.example.mall.module.order.dto.OrderDetailResponse;
import com.example.mall.module.order.dto.OrderSummaryResponse;

public interface OrderQueryService {

    PageResponse<OrderSummaryResponse> pageMerchantOrders(
        long pageNum,
        long pageSize,
        String platformCode,
        String orderStatus,
        String keyword
    );

    OrderDetailResponse merchantOrderDetail(Long orderId);
}
