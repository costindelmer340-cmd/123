package com.example.mall.module.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ticket")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ticketNo;
    private Long afterSaleId;
    private Long conversationId;
    private Long externalOrderId;
    private Long userId;
    private Long merchantId;
    private Long assignedStaffId;
    private String ticketType;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String aiCategory;
    private BigDecimal aiConfidence;
    private LocalDateTime dueAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
