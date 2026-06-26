package com.example.mall.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("faq_item")
public class FaqItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String question;
    private String answer;
    private String category;
    private Integer priority;
    private Integer enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
