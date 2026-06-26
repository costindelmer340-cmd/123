package com.example.mall.module.aftersale.dto;

import java.util.List;

public record AfterSaleDetailResponse(
    AfterSaleSummaryResponse afterSale,
    List<AfterSaleMaterialResponse> materials,
    List<RefundRecordResponse> refunds,
    List<ExternalAfterSaleMappingResponse> mappings
) {
}
