package com.example.mall.module.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ticket_record")
public class TicketRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long operatorId;
    private String actionType;
    private String fromStatus;
    private String toStatus;
    private String content;
    private LocalDateTime createdAt;
}
