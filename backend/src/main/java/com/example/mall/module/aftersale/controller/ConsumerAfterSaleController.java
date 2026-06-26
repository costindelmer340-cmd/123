package com.example.mall.module.aftersale.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.module.aftersale.dto.AfterSaleDetailResponse;
import com.example.mall.module.aftersale.dto.AfterSaleSummaryResponse;
import com.example.mall.module.aftersale.dto.CreateAfterSaleRequest;
import com.example.mall.module.aftersale.service.AfterSaleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consumer/after-sales")
public class ConsumerAfterSaleController {

    private final AfterSaleService afterSaleService;

    public ConsumerAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping
    public ApiResponse<AfterSaleSummaryResponse> create(@Valid @RequestBody CreateAfterSaleRequest request) {
        return ApiResponse.success(afterSaleService.createConsumerAfterSale(request), traceId());
    }

    @GetMapping("/{afterSaleId}")
    public ApiResponse<AfterSaleDetailResponse> detail(@PathVariable Long afterSaleId) {
        return ApiResponse.success(afterSaleService.afterSaleDetail(afterSaleId), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
