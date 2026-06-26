package com.example.mall.common.web;

import com.example.mall.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<String> health(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = String.valueOf(System.currentTimeMillis());
        }
        return ApiResponse.success("ok", traceId);
    }
}
