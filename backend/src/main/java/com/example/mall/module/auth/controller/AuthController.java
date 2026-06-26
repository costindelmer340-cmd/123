package com.example.mall.module.auth.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.module.auth.dto.AuthTokenResponse;
import com.example.mall.module.auth.dto.AuthUserResponse;
import com.example.mall.module.auth.dto.LoginRequest;
import com.example.mall.module.auth.dto.RefreshRequest;
import com.example.mall.module.auth.dto.RegisterRequest;
import com.example.mall.module.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), traceId());
    }

    @PostMapping("/register")
    public ApiResponse<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), traceId());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request), traceId());
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me() {
        return ApiResponse.success(authService.currentUser(), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
