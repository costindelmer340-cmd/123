package com.example.mall.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("customer_conversation")
public class CustomerConversation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationNo;
    private Long userId;
    private Long merchantId;
    private Long externalOrderId;
    private Long assignedStaffId;
    private String source;
    private String status;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private String aiIntent;
    private String aiSummary;
    private LocalDateTime transferredAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
