package com.example.mall.module.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.auth.LoginUser;
import com.example.mall.common.auth.SecurityUtils;
import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.customer.entity.CustomerConversation;
import com.example.mall.module.customer.mapper.CustomerConversationMapper;
import com.example.mall.module.ticket.dto.CreateTicketRequest;
import com.example.mall.module.ticket.dto.TicketDetailResponse;
import com.example.mall.module.ticket.dto.TicketRecordResponse;
import com.example.mall.module.ticket.dto.TicketSummaryResponse;
import com.example.mall.module.ticket.dto.UpdateTicketRequest;
import com.example.mall.module.ticket.entity.Ticket;
import com.example.mall.module.ticket.entity.TicketRecord;
import com.example.mall.module.ticket.mapper.TicketMapper;
import com.example.mall.module.ticket.mapper.TicketRecordMapper;
import com.example.mall.module.ticket.service.TicketService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final TicketRecordMapper ticketRecordMapper;
    private final CustomerConversationMapper conversationMapper;

    public TicketServiceImpl(
        TicketMapper ticketMapper,
        TicketRecordMapper ticketRecordMapper,
        CustomerConversationMapper conversationMapper
    ) {
        this.ticketMapper = ticketMapper;
        this.ticketRecordMapper = ticketRecordMapper;
        this.conversationMapper = conversationMapper;
    }

    @Override
    public PageResponse<TicketSummaryResponse> pageMerchantTickets(long pageNum, long pageSize, String status, String keyword) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Page<Ticket> page = ticketMapper.selectPage(
            new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
            new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getMerchantId, merchantId)
                .eq(status != null && !status.isBlank(), Ticket::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                    .like(Ticket::getTicketNo, keyword)
                    .or()
                    .like(Ticket::getTitle, keyword)
                    .or()
                    .like(Ticket::getDescription, keyword)
                )
                .orderByDesc(Ticket::getCreatedAt)
        );
        List<TicketSummaryResponse> records = page.getRecords().stream().map(this::toSummaryResponse).toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    public TicketDetailResponse ticketDetail(Long ticketId) {
        Ticket ticket = requireMerchantTicket(ticketId);
        List<TicketRecordResponse> records = ticketRecordMapper.selectList(
            new LambdaQueryWrapper<TicketRecord>()
                .eq(TicketRecord::getTicketId, ticketId)
                .orderByAsc(TicketRecord::getCreatedAt)
        ).stream().map(this::toRecordResponse).toList();
        return new TicketDetailResponse(toSummaryResponse(ticket), records);
    }

    @Override
    @Transactional
    public TicketSummaryResponse createTicket(CreateTicketRequest request) {
        LoginUser currentUser = SecurityUtils.currentUser();
        CustomerConversation conversation = conversationMapper.selectById(request.conversationId());
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "Conversation not found");
        }
        Ticket ticket = new Ticket();
        ticket.setTicketNo("TK" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(Locale.ROOT));
        ticket.setConversationId(conversation.getId());
        ticket.setExternalOrderId(conversation.getExternalOrderId());
        ticket.setUserId(conversation.getUserId());
        ticket.setMerchantId(conversation.getMerchantId());
        ticket.setAssignedStaffId(conversation.getAssignedStaffId());
        ticket.setTicketType(request.ticketType());
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setStatus("OPEN");
        ticket.setPriority(request.priority() == null || request.priority().isBlank() ? "NORMAL" : request.priority());
        ticket.setDueAt(LocalDateTime.now().plusDays(1));
        ticketMapper.insert(ticket);

        TicketRecord record = new TicketRecord();
        record.setTicketId(ticket.getId());
        record.setOperatorId(currentUser.getUserId());
        record.setActionType("CREATE");
        record.setFromStatus(null);
        record.setToStatus("OPEN");
        record.setContent(request.description());
        ticketRecordMapper.insert(record);
        return toSummaryResponse(ticket);
    }

    @Override
    @Transactional
    public TicketSummaryResponse updateTicket(Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = requireMerchantTicket(ticketId);
        String normalizedStatus = normalizeStatus(request.status());
        String fromStatus = ticket.getStatus();
        ticket.setStatus(normalizedStatus);
        if ("CLOSED".equals(normalizedStatus)) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        ticketMapper.updateById(ticket);

        TicketRecord record = new TicketRecord();
        record.setTicketId(ticket.getId());
        record.setOperatorId(SecurityUtils.currentUser().getUserId());
        record.setActionType("UPDATE_STATUS");
        record.setFromStatus(fromStatus);
        record.setToStatus(normalizedStatus);
        record.setContent(request.content());
        ticketRecordMapper.insert(record);
        return toSummaryResponse(ticket);
    }

    private Ticket requireMerchantTicket(Long ticketId) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || !merchantId.equals(ticket.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "Ticket not found");
        }
        return ticket;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("OPEN", "IN_PROGRESS", "PENDING", "RESOLVED", "CLOSED").contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "Unsupported ticket status: " + status);
        }
        return normalized;
    }

    private TicketSummaryResponse toSummaryResponse(Ticket ticket) {
        return new TicketSummaryResponse(
            ticket.getId(),
            ticket.getTicketNo(),
            ticket.getAfterSaleId(),
            ticket.getConversationId(),
            ticket.getExternalOrderId(),
            ticket.getUserId(),
            ticket.getMerchantId(),
            ticket.getAssignedStaffId(),
            ticket.getTicketType(),
            ticket.getTitle(),
            ticket.getStatus(),
            ticket.getPriority(),
            ticket.getAiCategory(),
            ticket.getAiConfidence(),
            ticket.getDueAt(),
            ticket.getClosedAt()
        );
    }

    private TicketRecordResponse toRecordResponse(TicketRecord record) {
        return new TicketRecordResponse(
            record.getId(),
            record.getTicketId(),
            record.getOperatorId(),
            record.getActionType(),
            record.getFromStatus(),
            record.getToStatus(),
            record.getContent(),
            record.getCreatedAt()
        );
    }
}
