package com.example.mall.module.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAfterSaleRequest(
    @NotNull(message = "外部订单ID不能为空")
    Long externalOrderId,

    Long externalOrderItemId,

    @NotBlank(message = "售后类型不能为空")
    String afterSaleType,

    @NotBlank(message = "原因类型不能为空")
    String reasonType,

    String problemDescription,

    @NotNull(message = "申请金额不能为空")
    BigDecimal requestedAmount
) {
}
