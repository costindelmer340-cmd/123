package com.example.mall.module.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record MockShopBindRequest(
    @NotBlank(message = "平台编码不能为空")
    String platformCode,

    @NotBlank(message = "外部店铺ID不能为空")
    String externalShopId,

    @NotBlank(message = "店铺名称不能为空")
    String shopName,

    String sellerNick
) {
}
