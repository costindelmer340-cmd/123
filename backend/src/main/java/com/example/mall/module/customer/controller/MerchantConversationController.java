package com.example.mall.module.customer.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.module.customer.dto.ChatMessageResponse;
import com.example.mall.module.customer.dto.ConversationSummaryResponse;
import com.example.mall.module.customer.dto.TransferConversationRequest;
import com.example.mall.module.customer.service.ConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/conversations")
public class MerchantConversationController {

    private final ConversationService conversationService;

    public MerchantConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ApiResponse<List<ConversationSummaryResponse>> list() {
        return ApiResponse.success(conversationService.pageMerchantConversations(), traceId());
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationSummaryResponse> detail(@PathVariable Long conversationId) {
        return ApiResponse.success(conversationService.conversationDetail(conversationId), traceId());
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(@PathVariable Long conversationId) {
        return ApiResponse.success(conversationService.listMessages(conversationId), traceId());
    }

    @PostMapping("/{conversationId}/transfer")
    public ApiResponse<ConversationSummaryResponse> transfer(
        @PathVariable Long conversationId,
        @Valid @RequestBody TransferConversationRequest request
    ) {
        return ApiResponse.success(conversationService.transferConversation(conversationId, request), traceId());
    }

    @PostMapping("/{conversationId}/close")
    public ApiResponse<Void> close(@PathVariable Long conversationId) {
        conversationService.closeConversation(conversationId);
        return ApiResponse.success(traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
