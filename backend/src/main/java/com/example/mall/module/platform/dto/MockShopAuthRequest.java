package com.example.mall.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record MockShopAuthRequest(
    @NotBlank(message = "授权码不能为空")
    String authCode,

    String scopeText,

    LocalDateTime accessTokenExpiresAt,

    LocalDateTime refreshTokenExpiresAt
) {
}
