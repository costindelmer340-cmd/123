package com.example.mall.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_order_item")
public class ExternalOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long externalOrderId;
    private String platformCode;
    private String externalItemId;
    private String externalProductId;
    private String productName;
    private String skuName;
    private String productImageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String afterSaleStatus;
    private String productSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
