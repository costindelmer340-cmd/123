package com.example.mall.module.sync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sync_cursor")
public class SyncCursor {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopBindingId;
    private String cursorType;
    private String cursorValue;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
