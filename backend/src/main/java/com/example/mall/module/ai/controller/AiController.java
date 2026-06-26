package com.example.mall.module.ai.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.module.ai.dto.AiReplyRequest;
import com.example.mall.module.ai.dto.AiTextRequest;
import com.example.mall.module.ai.dto.IntentResponse;
import com.example.mall.module.ai.dto.ReplyResponse;
import com.example.mall.module.ai.dto.SentimentResponse;
import com.example.mall.module.ai.dto.TicketClassifyResponse;
import com.example.mall.module.ai.dto.TopicResponse;
import com.example.mall.module.ai.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/intent")
    public ApiResponse<IntentResponse> intent(@Valid @RequestBody AiTextRequest request) {
        return ApiResponse.success(aiService.detectIntent(request), traceId());
    }

    @PostMapping("/sentiment")
    public ApiResponse<SentimentResponse> sentiment(@Valid @RequestBody AiTextRequest request) {
        return ApiResponse.success(aiService.analyzeSentiment(request), traceId());
    }

    @PostMapping("/topic")
    public ApiResponse<TopicResponse> topic(@Valid @RequestBody AiTextRequest request) {
        return ApiResponse.success(aiService.extractTopic(request), traceId());
    }

    @PostMapping("/ticket/classify")
    public ApiResponse<TicketClassifyResponse> ticketClassify(@Valid @RequestBody AiTextRequest request) {
        return ApiResponse.success(aiService.classifyTicket(request), traceId());
    }

    @PostMapping("/reply")
    public ApiResponse<ReplyResponse> reply(@Valid @RequestBody AiReplyRequest request) {
        return ApiResponse.success(aiService.generateReply(request), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
