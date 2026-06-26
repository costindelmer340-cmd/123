package com.example.mall.module.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("after_sale_write_back_log")
public class AfterSaleWriteBackLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long afterSaleId;
    private String platformCode;
    private Long shopBindingId;
    private String actionType;
    private String requestPayload;
    private String responsePayload;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
