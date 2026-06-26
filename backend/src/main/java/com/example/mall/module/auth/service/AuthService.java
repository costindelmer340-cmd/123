package com.example.mall.module.auth.service;

import com.example.mall.module.auth.dto.AuthTokenResponse;
import com.example.mall.module.auth.dto.AuthUserResponse;
import com.example.mall.module.auth.dto.LoginRequest;
import com.example.mall.module.auth.dto.RefreshRequest;
import com.example.mall.module.auth.dto.RegisterRequest;

public interface AuthService {

    AuthTokenResponse login(LoginRequest request);

    AuthUserResponse register(RegisterRequest request);

    AuthTokenResponse refresh(RefreshRequest request);

    AuthUserResponse currentUser();
}
