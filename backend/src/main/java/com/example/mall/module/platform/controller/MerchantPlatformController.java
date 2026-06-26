package com.example.mall.module.platform.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.module.platform.dto.MockShopAuthRequest;
import com.example.mall.module.platform.dto.MockShopBindRequest;
import com.example.mall.module.platform.dto.PlatformResponse;
import com.example.mall.module.platform.dto.ShopBindingResponse;
import com.example.mall.module.platform.service.PlatformBindingService;
import com.example.mall.module.sync.dto.SyncLogResponse;
import com.example.mall.module.sync.dto.SyncTaskResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/platform")
public class MerchantPlatformController {

    private final PlatformBindingService platformBindingService;

    public MerchantPlatformController(PlatformBindingService platformBindingService) {
        this.platformBindingService = platformBindingService;
    }

    @GetMapping("/platforms")
    public ApiResponse<List<PlatformResponse>> platforms() {
        return ApiResponse.success(platformBindingService.listEnabledPlatforms(), traceId());
    }

    @GetMapping("/shop-bindings")
    public ApiResponse<List<ShopBindingResponse>> shopBindings() {
        return ApiResponse.success(platformBindingService.listCurrentMerchantBindings(), traceId());
    }

    @PostMapping("/shop-bindings/mock-bind")
    public ApiResponse<ShopBindingResponse> mockBind(@Valid @RequestBody MockShopBindRequest request) {
        return ApiResponse.success(platformBindingService.mockBindShop(request), traceId());
    }

    @PostMapping("/shop-bindings/{bindingId}/mock-auth")
    public ApiResponse<ShopBindingResponse> mockAuth(
        @PathVariable Long bindingId,
        @Valid @RequestBody MockShopAuthRequest request
    ) {
        return ApiResponse.success(platformBindingService.mockAuthorizeShop(bindingId, request), traceId());
    }

    @GetMapping("/shop-bindings/{bindingId}/sync-tasks")
    public ApiResponse<List<SyncTaskResponse>> syncTasks(@PathVariable Long bindingId) {
        return ApiResponse.success(platformBindingService.listSyncTasks(bindingId), traceId());
    }

    @PostMapping("/shop-bindings/{bindingId}/sync/{syncType}/trigger")
    public ApiResponse<SyncLogResponse> triggerSync(@PathVariable Long bindingId, @PathVariable String syncType) {
        return ApiResponse.success(platformBindingService.triggerSync(bindingId, syncType), traceId());
    }

    @GetMapping("/shop-bindings/{bindingId}/sync-logs")
    public ApiResponse<List<SyncLogResponse>> syncLogs(
        @PathVariable Long bindingId,
        @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ApiResponse.success(platformBindingService.listSyncLogs(bindingId, limit), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
