package com.example.mall.module.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_platform")
public class ExternalPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String platformCode;
    private String platformName;
    private String apiBaseUrl;
    private String authBaseUrl;
    private Integer enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
