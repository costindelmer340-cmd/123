package com.example.mall.module.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("after_sale_material")
public class AfterSaleMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long afterSaleId;
    private Long userId;
    private String materialType;
    private String materialUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
