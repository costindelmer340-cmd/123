package com.example.mall.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "username is required") String username,
    @NotBlank(message = "password is required") @Size(min = 6, message = "password must be at least 6 characters") String password,
    @NotBlank(message = "nickname is required") String nickname,
    String phone,
    String email
) {
}
