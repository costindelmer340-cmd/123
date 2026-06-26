package com.example.mall.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long externalOrderId;
    private Long externalOrderItemId;
    private Long userId;
    private Long merchantId;
    private String platformCode;
    private String externalReviewId;
    private String reviewSource;
    private Integer productScore;
    private Integer logisticsScore;
    private Integer serviceScore;
    private String content;
    private String imageUrls;
    private Integer anonymous;
    private String status;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
