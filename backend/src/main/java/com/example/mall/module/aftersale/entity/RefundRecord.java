package com.example.mall.module.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundNo;
    private Long afterSaleId;
    private Long externalOrderId;
    private Long merchantId;
    private String platformCode;
    private String externalRefundNo;
    private BigDecimal refundAmount;
    private String refundStatus;
    private String reason;
    private LocalDateTime refundedAt;
    private String rawData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
