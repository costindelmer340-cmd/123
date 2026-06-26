package com.example.mall.module.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_after_sale_mapping")
public class ExternalAfterSaleMapping {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long afterSaleId;
    private String platformCode;
    private String externalAfterSaleNo;
    private String externalRefundNo;
    private String externalStatus;
    private String rawData;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
