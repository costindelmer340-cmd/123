package com.example.mall.module.auth.dto;

public record AuthTokenResponse(
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn,
    AuthUserResponse user
) {
}
