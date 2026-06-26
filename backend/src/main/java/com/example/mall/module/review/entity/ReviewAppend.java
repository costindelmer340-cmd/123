package com.example.mall.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_append")
public class ReviewAppend {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private String content;
    private String imageUrls;
    private LocalDateTime appendedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
