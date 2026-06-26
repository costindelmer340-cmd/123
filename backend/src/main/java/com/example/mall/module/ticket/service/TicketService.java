package com.example.mall.module.ticket.service;

import com.example.mall.common.response.PageResponse;
import com.example.mall.module.ticket.dto.CreateTicketRequest;
import com.example.mall.module.ticket.dto.TicketDetailResponse;
import com.example.mall.module.ticket.dto.TicketSummaryResponse;
import com.example.mall.module.ticket.dto.UpdateTicketRequest;

public interface TicketService {

    PageResponse<TicketSummaryResponse> pageMerchantTickets(long pageNum, long pageSize, String status, String keyword);

    TicketDetailResponse ticketDetail(Long ticketId);

    TicketSummaryResponse createTicket(CreateTicketRequest request);

    TicketSummaryResponse updateTicket(Long ticketId, UpdateTicketRequest request);
}
