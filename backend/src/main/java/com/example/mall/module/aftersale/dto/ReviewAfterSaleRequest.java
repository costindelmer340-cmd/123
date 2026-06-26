package com.example.mall.module.aftersale.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewAfterSaleRequest(
    @NotBlank(message = "处理结果不能为空")
    String status,

    String reviewOpinion,

    String finalResult
) {
}
