package com.example.mall.module.order.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.order.dto.OrderDetailResponse;
import com.example.mall.module.order.dto.OrderSummaryResponse;
import com.example.mall.module.order.service.OrderQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/orders")
public class MerchantOrderController {

    private final OrderQueryService orderQueryService;

    public MerchantOrderController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> pageOrders(
        @RequestParam(defaultValue = "1") long pageNum,
        @RequestParam(defaultValue = "10") long pageSize,
        @RequestParam(required = false) String platformCode,
        @RequestParam(required = false) String orderStatus,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(orderQueryService.pageMerchantOrders(pageNum, pageSize, platformCode, orderStatus, keyword), traceId());
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable Long orderId) {
        return ApiResponse.success(orderQueryService.merchantOrderDetail(orderId), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
