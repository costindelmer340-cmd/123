package com.example.mall.module.aftersale.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.aftersale.dto.AfterSaleDetailResponse;
import com.example.mall.module.aftersale.dto.AfterSaleSummaryResponse;
import com.example.mall.module.aftersale.dto.ReviewAfterSaleRequest;
import com.example.mall.module.aftersale.service.AfterSaleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/after-sales")
public class MerchantAfterSaleController {

    private final AfterSaleService afterSaleService;

    public MerchantAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AfterSaleSummaryResponse>> page(
        @RequestParam(defaultValue = "1") long pageNum,
        @RequestParam(defaultValue = "10") long pageSize,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(afterSaleService.pageMerchantAfterSales(pageNum, pageSize, status, keyword), traceId());
    }

    @GetMapping("/{afterSaleId}")
    public ApiResponse<AfterSaleDetailResponse> detail(@PathVariable Long afterSaleId) {
        return ApiResponse.success(afterSaleService.afterSaleDetail(afterSaleId), traceId());
    }

    @PostMapping("/{afterSaleId}/review")
    public ApiResponse<AfterSaleSummaryResponse> review(
        @PathVariable Long afterSaleId,
        @Valid @RequestBody ReviewAfterSaleRequest request
    ) {
        return ApiResponse.success(afterSaleService.reviewAfterSale(afterSaleId, request), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
