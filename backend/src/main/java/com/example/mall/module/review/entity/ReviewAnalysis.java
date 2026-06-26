package com.example.mall.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_analysis")
public class ReviewAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private String sentiment;
    private BigDecimal sentimentScore;
    private String topics;
    private String keywords;
    private String riskLevel;
    private String summary;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
