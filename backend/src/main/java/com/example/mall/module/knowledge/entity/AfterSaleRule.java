package com.example.mall.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("after_sale_rule")
public class AfterSaleRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String ruleName;
    private String ruleType;
    private String conditionsJson;
    private String actionJson;
    private String content;
    private Integer enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
