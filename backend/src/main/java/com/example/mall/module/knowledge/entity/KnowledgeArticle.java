package com.example.mall.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_article")
public class KnowledgeArticle {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String title;
    private String content;
    private String category;
    private String tags;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
