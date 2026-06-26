package com.example.mall.module.customer.controller;

import com.example.mall.module.customer.dto.ChatMessageRequest;
import com.example.mall.module.customer.dto.ChatMessageResponse;
import com.example.mall.module.customer.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ConversationService conversationService;

    public ChatWebSocketController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @MessageMapping("/chat.send")
    public ChatMessageResponse send(@Valid @Payload ChatMessageRequest request) {
        return conversationService.sendMessage(request, "CONSUMER");
    }

    @MessageMapping("/chat.staff.send")
    public ChatMessageResponse staffSend(@Valid @Payload ChatMessageRequest request) {
        return conversationService.sendMessage(request, "CUSTOMER_SERVICE");
    }
}
