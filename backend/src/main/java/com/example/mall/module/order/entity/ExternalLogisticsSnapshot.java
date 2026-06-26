package com.example.mall.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_logistics_snapshot")
public class ExternalLogisticsSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long externalOrderId;
    private String platformCode;
    private String logisticsCompany;
    private String trackingNo;
    private String logisticsStatus;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    private String trackingDetail;
    private String rawData;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
