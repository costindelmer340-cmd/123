package com.example.mall.module.platform.dto;

public record PlatformResponse(
    Long id,
    String platformCode,
    String platformName,
    String apiBaseUrl,
    String authBaseUrl,
    Boolean enabled,
    String description
) {
}
