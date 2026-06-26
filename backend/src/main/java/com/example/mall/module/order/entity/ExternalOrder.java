package com.example.mall.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_order")
public class ExternalOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopBindingId;
    private Long merchantId;
    private String platformCode;
    private String externalOrderNo;
    private String buyerMaskedName;
    private String buyerMaskedPhone;
    private String orderStatus;
    private String payStatus;
    private String logisticsStatus;
    private String afterSaleStatus;
    private BigDecimal totalAmount;
    private BigDecimal payableAmount;
    private LocalDateTime paidAt;
    private LocalDateTime orderedAt;
    private LocalDateTime completedAt;
    private String rawData;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
