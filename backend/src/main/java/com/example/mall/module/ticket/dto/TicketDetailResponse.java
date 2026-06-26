package com.example.mall.module.ticket.dto;

import java.util.List;

public record TicketDetailResponse(
    TicketSummaryResponse ticket,
    List<TicketRecordResponse> records
) {
}
