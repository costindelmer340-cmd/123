package com.example.mall.module.sync.dto;

import java.time.LocalDateTime;

public record SyncTaskResponse(
    Long id,
    Long shopBindingId,
    String taskType,
    String taskName,
    String scheduleCron,
    Boolean enabled,
    LocalDateTime lastRunAt,
    LocalDateTime nextRunAt
) {
}
