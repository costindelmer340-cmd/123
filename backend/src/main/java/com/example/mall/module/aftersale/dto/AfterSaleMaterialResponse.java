package com.example.mall.module.aftersale.dto;

import java.time.LocalDateTime;

public record AfterSaleMaterialResponse(
    Long id,
    Long userId,
    String materialType,
    String materialUrl,
    String description,
    LocalDateTime createdAt
) {
}
