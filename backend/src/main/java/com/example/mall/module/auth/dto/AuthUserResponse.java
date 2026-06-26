package com.example.mall.module.auth.dto;

import java.util.Set;

public record AuthUserResponse(
    Long userId,
    String username,
    String nickname,
    Long merchantId,
    Set<String> roles
) {
}
