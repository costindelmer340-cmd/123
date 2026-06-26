package com.example.mall.common.handler;

import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage(), traceId(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldError() == null
            ? ErrorCode.BAD_REQUEST.getMessage()
            : ex.getBindingResult().getFieldError().getDefaultMessage();
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), message, traceId(request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage(), traceId(request));
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknownException(Exception ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.error("Unhandled exception, traceId={}, path={}", traceId, request.getRequestURI(), ex);
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), message, traceId);
    }

    private String traceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        return (traceId == null || traceId.isBlank()) ? String.valueOf(System.currentTimeMillis()) : traceId;
    }
}
