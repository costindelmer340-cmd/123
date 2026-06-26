package com.example.mall.module.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mall.common.auth.SecurityUtils;
import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.module.platform.dto.MockShopAuthRequest;
import com.example.mall.module.platform.dto.MockShopBindRequest;
import com.example.mall.module.platform.dto.PlatformResponse;
import com.example.mall.module.platform.dto.ShopBindingResponse;
import com.example.mall.module.platform.entity.ExternalApiCallLog;
import com.example.mall.module.platform.entity.ExternalAuthToken;
import com.example.mall.module.platform.entity.ExternalPlatform;
import com.example.mall.module.platform.entity.ExternalShopBinding;
import com.example.mall.module.platform.mapper.ExternalApiCallLogMapper;
import com.example.mall.module.platform.mapper.ExternalAuthTokenMapper;
import com.example.mall.module.platform.mapper.ExternalPlatformMapper;
import com.example.mall.module.platform.mapper.ExternalShopBindingMapper;
import com.example.mall.module.platform.service.PlatformBindingService;
import com.example.mall.module.sync.dto.SyncLogResponse;
import com.example.mall.module.sync.dto.SyncTaskResponse;
import com.example.mall.module.sync.entity.SyncCursor;
import com.example.mall.module.sync.entity.SyncLog;
import com.example.mall.module.sync.entity.SyncTask;
import com.example.mall.module.sync.mapper.SyncCursorMapper;
import com.example.mall.module.sync.mapper.SyncLogMapper;
import com.example.mall.module.sync.mapper.SyncTaskMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformBindingServiceImpl implements PlatformBindingService {

    private static final Map<String, String> DEFAULT_TASK_NAMES = Map.of(
        "ORDER_SYNC", "外部订单同步",
        "AFTER_SALE_SYNC", "外部售后同步",
        "REVIEW_SYNC", "外部评价同步"
    );

    private static final Map<String, String> DEFAULT_TASK_CRONS = Map.of(
        "ORDER_SYNC", "0 */30 * * * ?",
        "AFTER_SALE_SYNC", "0 */10 * * * ?",
        "REVIEW_SYNC", "0 0 */2 * * ?"
    );

    private final ExternalPlatformMapper externalPlatformMapper;
    private final ExternalShopBindingMapper externalShopBindingMapper;
    private final ExternalAuthTokenMapper externalAuthTokenMapper;
    private final ExternalApiCallLogMapper externalApiCallLogMapper;
    private final SyncTaskMapper syncTaskMapper;
    private final SyncCursorMapper syncCursorMapper;
    private final SyncLogMapper syncLogMapper;

    public PlatformBindingServiceImpl(
        ExternalPlatformMapper externalPlatformMapper,
        ExternalShopBindingMapper externalShopBindingMapper,
        ExternalAuthTokenMapper externalAuthTokenMapper,
        ExternalApiCallLogMapper externalApiCallLogMapper,
        SyncTaskMapper syncTaskMapper,
        SyncCursorMapper syncCursorMapper,
        SyncLogMapper syncLogMapper
    ) {
        this.externalPlatformMapper = externalPlatformMapper;
        this.externalShopBindingMapper = externalShopBindingMapper;
        this.externalAuthTokenMapper = externalAuthTokenMapper;
        this.externalApiCallLogMapper = externalApiCallLogMapper;
        this.syncTaskMapper = syncTaskMapper;
        this.syncCursorMapper = syncCursorMapper;
        this.syncLogMapper = syncLogMapper;
    }

    @Override
    public List<PlatformResponse> listEnabledPlatforms() {
        return externalPlatformMapper.selectList(
            new LambdaQueryWrapper<ExternalPlatform>()
                .eq(ExternalPlatform::getEnabled, 1)
                .orderByAsc(ExternalPlatform::getId)
        ).stream().map(this::toPlatformResponse).toList();
    }

    @Override
    public List<ShopBindingResponse> listCurrentMerchantBindings() {
        Long merchantId = SecurityUtils.currentMerchantId();
        return externalShopBindingMapper.selectList(
            new LambdaQueryWrapper<ExternalShopBinding>()
                .eq(ExternalShopBinding::getMerchantId, merchantId)
                .orderByDesc(ExternalShopBinding::getCreatedAt)
        ).stream().map(this::toShopBindingResponse).toList();
    }

    @Override
    @Transactional
    public ShopBindingResponse mockBindShop(MockShopBindRequest request) {
        Long merchantId = SecurityUtils.currentMerchantId();
        ExternalPlatform platform = externalPlatformMapper.selectOne(
            new LambdaQueryWrapper<ExternalPlatform>()
                .eq(ExternalPlatform::getPlatformCode, request.platformCode())
                .eq(ExternalPlatform::getEnabled, 1)
                .last("limit 1")
        );
        if (platform == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "External platform not found or disabled");
        }

        ExternalShopBinding existing = externalShopBindingMapper.selectOne(
            new LambdaQueryWrapper<ExternalShopBinding>()
                .eq(ExternalShopBinding::getPlatformCode, request.platformCode())
                .eq(ExternalShopBinding::getExternalShopId, request.externalShopId())
                .last("limit 1")
        );
        if (existing != null) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "External shop has already been bound");
        }

        ExternalShopBinding binding = new ExternalShopBinding();
        binding.setMerchantId(merchantId);
        binding.setPlatformId(platform.getId());
        binding.setPlatformCode(platform.getPlatformCode());
        binding.setExternalShopId(request.externalShopId());
        binding.setShopName(request.shopName());
        binding.setSellerNick(request.sellerNick());
        binding.setAuthStatus("PENDING_AUTH");
        externalShopBindingMapper.insert(binding);

        createDefaultSyncTasks(binding);
        logApiCall(binding.getPlatformCode(), binding.getId(), "mock.shop.bind", "SHOP_BINDING", binding.getId(), "模拟绑定外部店铺", "绑定成功", true, null, 20);
        return toShopBindingResponse(binding);
    }

    @Override
    @Transactional
    public ShopBindingResponse mockAuthorizeShop(Long bindingId, MockShopAuthRequest request) {
        ExternalShopBinding binding = currentMerchantBinding(bindingId);

        ExternalAuthToken token = new ExternalAuthToken();
        token.setShopBindingId(binding.getId());
        token.setAccessTokenCipher(mockCipher("access:" + request.authCode()));
        token.setRefreshTokenCipher(mockCipher("refresh:" + request.authCode()));
        token.setAccessTokenExpiresAt(request.accessTokenExpiresAt() == null ? LocalDateTime.now().plusDays(30) : request.accessTokenExpiresAt());
        token.setRefreshTokenExpiresAt(request.refreshTokenExpiresAt() == null ? LocalDateTime.now().plusDays(180) : request.refreshTokenExpiresAt());
        token.setScopeText(request.scopeText() == null || request.scopeText().isBlank() ? "order,after_sale,review,logistics" : request.scopeText());
        token.setStatus("ACTIVE");
        externalAuthTokenMapper.insert(token);

        binding.setAuthStatus("ACTIVE");
        externalShopBindingMapper.updateById(binding);

        logApiCall(binding.getPlatformCode(), binding.getId(), "mock.shop.auth", "SHOP_BINDING", binding.getId(), "模拟授权码换取 Token", "授权成功", true, null, 35);
        return toShopBindingResponse(binding);
    }

    @Override
    public List<SyncTaskResponse> listSyncTasks(Long bindingId) {
        currentMerchantBinding(bindingId);
        return syncTaskMapper.selectList(
            new LambdaQueryWrapper<SyncTask>()
                .eq(SyncTask::getShopBindingId, bindingId)
                .orderByAsc(SyncTask::getId)
        ).stream().map(this::toSyncTaskResponse).toList();
    }

    @Override
    @Transactional
    public SyncLogResponse triggerSync(Long bindingId, String syncType) {
        ExternalShopBinding binding = currentMerchantBinding(bindingId);
        String normalizedSyncType = normalizeSyncType(syncType);
        SyncTask task = resolveSyncTask(binding, normalizedSyncType);

        LocalDateTime now = LocalDateTime.now();
        SyncLog log = new SyncLog();
        log.setTaskId(task.getId());
        log.setShopBindingId(binding.getId());
        log.setSyncType(normalizedSyncType);
        log.setStatus("SUCCESS");
        log.setStartTime(now);
        log.setEndTime(now.plusSeconds(1));
        log.setTotalCount(0);
        log.setSuccessCount(0);
        log.setFailedCount(0);
        syncLogMapper.insert(log);

        task.setLastRunAt(now);
        task.setNextRunAt(nextRunTime(normalizedSyncType, now));
        syncTaskMapper.updateById(task);

        upsertCursor(binding.getId(), normalizedSyncType, now);
        binding.setLastSyncedAt(now);
        externalShopBindingMapper.updateById(binding);

        logApiCall(binding.getPlatformCode(), binding.getId(), "mock.sync." + normalizedSyncType.toLowerCase(), "SYNC_TASK", task.getId(), "手动触发同步任务", "模拟同步成功，真实平台接入后写入订单/售后/评价数据", true, null, 80);
        return toSyncLogResponse(log);
    }

    @Override
    public List<SyncLogResponse> listSyncLogs(Long bindingId, Integer limit) {
        currentMerchantBinding(bindingId);
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
        return syncLogMapper.selectList(
            new LambdaQueryWrapper<SyncLog>()
                .eq(SyncLog::getShopBindingId, bindingId)
                .orderByDesc(SyncLog::getCreatedAt)
                .last("limit " + safeLimit)
        ).stream().map(this::toSyncLogResponse).toList();
    }

    private ExternalShopBinding currentMerchantBinding(Long bindingId) {
        Long merchantId = SecurityUtils.currentMerchantId();
        ExternalShopBinding binding = externalShopBindingMapper.selectById(bindingId);
        if (binding == null || !merchantId.equals(binding.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "Shop binding not found");
        }
        return binding;
    }

    private void createDefaultSyncTasks(ExternalShopBinding binding) {
        DEFAULT_TASK_NAMES.forEach((taskType, taskName) -> {
            SyncTask task = new SyncTask();
            task.setShopBindingId(binding.getId());
            task.setTaskType(taskType);
            task.setTaskName(binding.getPlatformCode() + taskName);
            task.setScheduleCron(DEFAULT_TASK_CRONS.get(taskType));
            task.setEnabled(1);
            syncTaskMapper.insert(task);
        });
    }

    private SyncTask resolveSyncTask(ExternalShopBinding binding, String syncType) {
        SyncTask task = syncTaskMapper.selectOne(
            new LambdaQueryWrapper<SyncTask>()
                .eq(SyncTask::getShopBindingId, binding.getId())
                .eq(SyncTask::getTaskType, syncType)
                .last("limit 1")
        );
        if (task != null) {
            return task;
        }
        task = new SyncTask();
        task.setShopBindingId(binding.getId());
        task.setTaskType(syncType);
        task.setTaskName(binding.getPlatformCode() + DEFAULT_TASK_NAMES.get(syncType));
        task.setScheduleCron(DEFAULT_TASK_CRONS.get(syncType));
        task.setEnabled(1);
        syncTaskMapper.insert(task);
        return task;
    }

    private void upsertCursor(Long bindingId, String syncType, LocalDateTime now) {
        SyncCursor cursor = syncCursorMapper.selectOne(
            new LambdaQueryWrapper<SyncCursor>()
                .eq(SyncCursor::getShopBindingId, bindingId)
                .eq(SyncCursor::getCursorType, syncType)
                .last("limit 1")
        );
        if (cursor == null) {
            cursor = new SyncCursor();
            cursor.setShopBindingId(bindingId);
            cursor.setCursorType(syncType);
            cursor.setCursorValue("mock-cursor-" + now);
            cursor.setLastSyncTime(now);
            syncCursorMapper.insert(cursor);
            return;
        }
        cursor.setCursorValue("mock-cursor-" + now);
        cursor.setLastSyncTime(now);
        syncCursorMapper.updateById(cursor);
    }

    private String normalizeSyncType(String syncType) {
        String normalized = syncType == null ? "" : syncType.trim().toUpperCase();
        if (!DEFAULT_TASK_NAMES.containsKey(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "Unsupported sync type: " + syncType);
        }
        return normalized;
    }

    private LocalDateTime nextRunTime(String syncType, LocalDateTime baseTime) {
        return switch (syncType) {
            case "AFTER_SALE_SYNC" -> baseTime.plusMinutes(10);
            case "REVIEW_SYNC" -> baseTime.plusHours(2);
            default -> baseTime.plusMinutes(30);
        };
    }

    private void logApiCall(
        String platformCode,
        Long bindingId,
        String apiName,
        String businessType,
        Long businessId,
        String requestSummary,
        String responseSummary,
        boolean success,
        String errorMessage,
        Integer latencyMs
    ) {
        ExternalApiCallLog log = new ExternalApiCallLog();
        log.setPlatformCode(platformCode);
        log.setShopBindingId(bindingId);
        log.setApiName(apiName);
        log.setBusinessType(businessType);
        log.setBusinessId(businessId);
        log.setRequestSummary(requestSummary);
        log.setResponseSummary(responseSummary);
        log.setSuccess(success ? 1 : 0);
        log.setErrorMessage(errorMessage);
        log.setLatencyMs(latencyMs);
        externalApiCallLogMapper.insert(log);
    }

    private String mockCipher(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private PlatformResponse toPlatformResponse(ExternalPlatform platform) {
        return new PlatformResponse(
            platform.getId(),
            platform.getPlatformCode(),
            platform.getPlatformName(),
            platform.getApiBaseUrl(),
            platform.getAuthBaseUrl(),
            platform.getEnabled() != null && platform.getEnabled() == 1,
            platform.getDescription()
        );
    }

    private ShopBindingResponse toShopBindingResponse(ExternalShopBinding binding) {
        return new ShopBindingResponse(
            binding.getId(),
            binding.getMerchantId(),
            binding.getPlatformId(),
            binding.getPlatformCode(),
            binding.getExternalShopId(),
            binding.getShopName(),
            binding.getSellerNick(),
            binding.getAuthStatus(),
            binding.getLastSyncedAt(),
            binding.getCreatedAt()
        );
    }

    private SyncTaskResponse toSyncTaskResponse(SyncTask task) {
        return new SyncTaskResponse(
            task.getId(),
            task.getShopBindingId(),
            task.getTaskType(),
            task.getTaskName(),
            task.getScheduleCron(),
            task.getEnabled() != null && task.getEnabled() == 1,
            task.getLastRunAt(),
            task.getNextRunAt()
        );
    }

    private SyncLogResponse toSyncLogResponse(SyncLog log) {
        return new SyncLogResponse(
            log.getId(),
            log.getTaskId(),
            log.getShopBindingId(),
            log.getSyncType(),
            log.getStatus(),
            log.getStartTime(),
            log.getEndTime(),
            log.getTotalCount(),
            log.getSuccessCount(),
            log.getFailedCount(),
            log.getErrorMessage(),
            log.getCreatedAt()
        );
    }
}
