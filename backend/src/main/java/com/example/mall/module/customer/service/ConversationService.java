package com.example.mall.module.customer.service;

import com.example.mall.module.customer.dto.ChatMessageRequest;
import com.example.mall.module.customer.dto.ChatMessageResponse;
import com.example.mall.module.customer.dto.ConversationSummaryResponse;
import com.example.mall.module.customer.dto.ServiceEvaluationRequest;
import com.example.mall.module.customer.dto.StartConversationRequest;
import com.example.mall.module.customer.dto.TransferConversationRequest;
import java.util.List;

public interface ConversationService {

    ConversationSummaryResponse startConversation(StartConversationRequest request);

    List<ConversationSummaryResponse> pageMerchantConversations();

    ConversationSummaryResponse conversationDetail(Long conversationId);

    ChatMessageResponse sendMessage(ChatMessageRequest request, String senderType);

    ConversationSummaryResponse transferConversation(Long conversationId, TransferConversationRequest request);

    void closeConversation(Long conversationId);

    void evaluateConversation(Long conversationId, ServiceEvaluationRequest request);

    List<ChatMessageResponse> listMessages(Long conversationId);
}
