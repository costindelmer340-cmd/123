package com.example.mall.module.customer.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.module.customer.dto.ChatMessageResponse;
import com.example.mall.module.customer.dto.ConversationSummaryResponse;
import com.example.mall.module.customer.dto.ServiceEvaluationRequest;
import com.example.mall.module.customer.dto.StartConversationRequest;
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
@RequestMapping("/api/consumer/conversations")
public class ConsumerConversationController {

    private final ConversationService conversationService;

    public ConsumerConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ApiResponse<ConversationSummaryResponse> start(@Valid @RequestBody StartConversationRequest request) {
        return ApiResponse.success(conversationService.startConversation(request), traceId());
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(@PathVariable Long conversationId) {
        return ApiResponse.success(conversationService.listMessages(conversationId), traceId());
    }

    @PostMapping("/{conversationId}/evaluate")
    public ApiResponse<Void> evaluate(@PathVariable Long conversationId, @Valid @RequestBody ServiceEvaluationRequest request) {
        conversationService.evaluateConversation(conversationId, request);
        return ApiResponse.success(traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
