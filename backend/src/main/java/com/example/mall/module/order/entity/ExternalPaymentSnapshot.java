package com.example.mall.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_payment_snapshot")
public class ExternalPaymentSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long externalOrderId;
    private String platformCode;
    private String externalPaymentNo;
    private String payChannel;
    private String payStatus;
    private BigDecimal paidAmount;
    private LocalDateTime paidAt;
    private String rawData;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
