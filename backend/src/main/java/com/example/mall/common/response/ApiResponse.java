package com.example.mall.common.response;

public record ApiResponse<T>(String code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("200", "success", data, traceId);
    }

    public static <T> ApiResponse<T> success(String traceId) {
        return success(null, traceId);
    }

    public static <T> ApiResponse<T> fail(String code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
