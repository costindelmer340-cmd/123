package com.example.mall.common.auth;

import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "Unauthorized");
    }

    public static Long currentMerchantId() {
        Long merchantId = currentUser().getMerchantId();
        if (merchantId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "Current user is not bound to a merchant");
        }
        return merchantId;
    }
}
