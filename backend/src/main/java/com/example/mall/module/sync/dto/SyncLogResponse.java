package com.example.mall.module.sync.dto;

import java.time.LocalDateTime;

public record SyncLogResponse(
    Long id,
    Long taskId,
    Long shopBindingId,
    String syncType,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer totalCount,
    Integer successCount,
    Integer failedCount,
    String errorMessage,
    LocalDateTime createdAt
) {
}
