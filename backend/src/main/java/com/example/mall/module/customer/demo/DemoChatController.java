package com.example.mall.module.customer.demo;

import com.example.mall.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/demo-chat")
public class DemoChatController {

    private final DemoChatStore chatStore;

    public DemoChatController(DemoChatStore chatStore) {
        this.chatStore = chatStore;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<DemoConversationResponse>> conversations() {
        return ApiResponse.success(chatStore.listConversations(), traceId());
    }

    @GetMapping("/conversations/{orderNo}")
    public ApiResponse<DemoConversationResponse> conversation(@PathVariable String orderNo) {
        return ApiResponse.success(chatStore.getConversation(orderNo), traceId());
    }

    @GetMapping("/conversations/{orderNo}/messages")
    public ApiResponse<List<DemoChatMessageResponse>> messages(@PathVariable String orderNo) {
        return ApiResponse.success(chatStore.listMessages(orderNo), traceId());
    }

    @PostMapping("/conversations/{orderNo}/messages")
    public ApiResponse<DemoChatMessageResponse> send(
        @PathVariable String orderNo,
        @Valid @RequestBody DemoChatMessageRequest request
    ) {
        return ApiResponse.success(chatStore.addMessage(orderNo, request), traceId());
    }

    @PostMapping("/conversations/{orderNo}/transfer")
    public ApiResponse<DemoConversationResponse> transfer(@PathVariable String orderNo) {
        return ApiResponse.success(chatStore.transferToStaff(orderNo), traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }

    public record DemoChatMessageRequest(
        @NotBlank(message = "发送方不能为空")
        String senderType,
        @NotBlank(message = "消息内容不能为空")
        String content
    ) {
    }

    public record DemoConversationResponse(
        Long id,
        String conversationNo,
        String orderNo,
        String productName,
        String merchantName,
        String afterSaleStatus,
        String status,
        String aiIntent,
        String lastMessage,
        String lastMessageAt
    ) {
    }

    public record DemoChatMessageResponse(
        String id,
        String orderNo,
        String senderType,
        String speaker,
        String content,
        String createdAt
    ) {
    }
}
