package com.example.mall.module.aftersale.service;

import com.example.mall.common.response.PageResponse;
import com.example.mall.module.aftersale.dto.AfterSaleDetailResponse;
import com.example.mall.module.aftersale.dto.AfterSaleSummaryResponse;
import com.example.mall.module.aftersale.dto.CreateAfterSaleRequest;
import com.example.mall.module.aftersale.dto.ReviewAfterSaleRequest;

public interface AfterSaleService {

    PageResponse<AfterSaleSummaryResponse> pageMerchantAfterSales(long pageNum, long pageSize, String status, String keyword);

    AfterSaleDetailResponse afterSaleDetail(Long afterSaleId);

    AfterSaleSummaryResponse createConsumerAfterSale(CreateAfterSaleRequest request);

    AfterSaleSummaryResponse reviewAfterSale(Long afterSaleId, ReviewAfterSaleRequest request);
}
