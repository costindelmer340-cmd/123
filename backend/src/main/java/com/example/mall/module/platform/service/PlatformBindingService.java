package com.example.mall.module.platform.service;

import com.example.mall.module.platform.dto.MockShopAuthRequest;
import com.example.mall.module.platform.dto.MockShopBindRequest;
import com.example.mall.module.platform.dto.PlatformResponse;
import com.example.mall.module.platform.dto.ShopBindingResponse;
import com.example.mall.module.sync.dto.SyncLogResponse;
import com.example.mall.module.sync.dto.SyncTaskResponse;
import java.util.List;

public interface PlatformBindingService {

    List<PlatformResponse> listEnabledPlatforms();

    List<ShopBindingResponse> listCurrentMerchantBindings();

    ShopBindingResponse mockBindShop(MockShopBindRequest request);

    ShopBindingResponse mockAuthorizeShop(Long bindingId, MockShopAuthRequest request);

    List<SyncTaskResponse> listSyncTasks(Long bindingId);

    SyncLogResponse triggerSync(Long bindingId, String syncType);

    List<SyncLogResponse> listSyncLogs(Long bindingId, Integer limit);
}
