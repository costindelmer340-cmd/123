package com.example.mall.module.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_shop_binding")
public class ExternalShopBinding {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long platformId;
    private String platformCode;
    private String externalShopId;
    private String shopName;
    private String sellerNick;
    private String authStatus;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
