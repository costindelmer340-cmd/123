package com.example.mall.module.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("external_api_call_log")
public class ExternalApiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String platformCode;
    private Long shopBindingId;
    private String apiName;
    private String businessType;
    private Long businessId;
    private String requestSummary;
    private String responseSummary;
    private Integer success;
    private String errorMessage;
    private Integer latencyMs;
    private LocalDateTime createdAt;
}
