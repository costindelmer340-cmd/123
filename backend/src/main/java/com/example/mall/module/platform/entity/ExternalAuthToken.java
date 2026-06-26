package com.example.mall.module.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_auth_token")
public class ExternalAuthToken {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopBindingId;
    private String accessTokenCipher;
    private String refreshTokenCipher;
    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private String scopeText;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
