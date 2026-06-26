package com.example.mall.module.ticket.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.ticket.dto.CreateTicketRequest;
import com.example.mall.module.ticket.dto.TicketDetailResponse;
import com.example.mall.module.ticket.dto.TicketSummaryResponse;
import com.example.mall.module.ticket.dto.UpdateTicketRequest;
import com.example.mall.module.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/tickets")
public class MerchantTicketController {

    private final TicketService ticketService;

    public MerchantTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ApiResponse<PageResponse<TicketSummaryResponse>> page(
        @RequestParam(defaultValue = "1") long pageNum,
        @RequestParam(defaultValue = "10") long pageSize,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(ticketService.pageMerchantTickets(pageNum, pageSize, status, keyword), traceId());
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDetailResponse> detail(@PathVariable Long ticketId) {
        return ApiResponse.success(ticketService.ticketDetail(ticketId), traceId());
    }

    @PostMapping
    public ApiResponse<TicketSummaryResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        return ApiResponse.success(ticketService.createTicket(request), traceId());
    }

    @PutMapping("/{ticketId}")
    public ApiResponse<TicketSummaryResponse> update(@PathVariable Long ticketId, @Valid @RequestBody UpdateTicketRequest request) {
        return ApiResponse.success(ticketService.updateTicket(ticketId, request), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
