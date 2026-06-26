package com.example.mall.module.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("after_sale_application")
public class AfterSaleApplication {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String afterSaleNo;
    private Long externalOrderId;
    private Long externalOrderItemId;
    private Long userId;
    private Long merchantId;
    private Long shopBindingId;
    private String afterSaleType;
    private String reasonType;
    private String problemDescription;
    private BigDecimal requestedAmount;
    private String status;
    private String priority;
    private String aiCategory;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private String reviewOpinion;
    private String finalResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
