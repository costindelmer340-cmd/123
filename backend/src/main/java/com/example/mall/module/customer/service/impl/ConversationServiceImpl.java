package com.example.mall.module.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mall.common.auth.LoginUser;
import com.example.mall.common.auth.SecurityUtils;
import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.module.ai.dto.AiReplyRequest;
import com.example.mall.module.ai.dto.ReplyResponse;
import com.example.mall.module.ai.service.AiService;
import com.example.mall.module.customer.dto.ChatMessageRequest;
import com.example.mall.module.customer.dto.ChatMessageResponse;
import com.example.mall.module.customer.dto.ConversationSummaryResponse;
import com.example.mall.module.customer.dto.ServiceEvaluationRequest;
import com.example.mall.module.customer.dto.StartConversationRequest;
import com.example.mall.module.customer.dto.TransferConversationRequest;
import com.example.mall.module.customer.entity.ChatMessage;
import com.example.mall.module.customer.entity.CustomerConversation;
import com.example.mall.module.customer.entity.ServiceEvaluation;
import com.example.mall.module.customer.mapper.ChatMessageMapper;
import com.example.mall.module.customer.mapper.CustomerConversationMapper;
import com.example.mall.module.customer.mapper.ServiceEvaluationMapper;
import com.example.mall.module.customer.service.ConversationService;
import com.example.mall.module.merchant.mapper.MerchantStaffMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final CustomerConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ServiceEvaluationMapper serviceEvaluationMapper;
    private final MerchantStaffMapper merchantStaffMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiService aiService;

    public ConversationServiceImpl(
        CustomerConversationMapper conversationMapper,
        ChatMessageMapper chatMessageMapper,
        ServiceEvaluationMapper serviceEvaluationMapper,
        MerchantStaffMapper merchantStaffMapper,
        SimpMessagingTemplate messagingTemplate,
        AiService aiService
    ) {
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.serviceEvaluationMapper = serviceEvaluationMapper;
        this.merchantStaffMapper = merchantStaffMapper;
        this.messagingTemplate = messagingTemplate;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public ConversationSummaryResponse startConversation(StartConversationRequest request) {
        LoginUser currentUser = SecurityUtils.currentUser();
        CustomerConversation conversation = new CustomerConversation();
        conversation.setConversationNo("CV" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(Locale.ROOT));
        conversation.setUserId(currentUser.getUserId());
        conversation.setMerchantId(request.merchantId());
        conversation.setExternalOrderId(request.externalOrderId());
        conversation.setSource("WEB");
        conversation.setStatus("AI_SERVING");
        conversationMapper.insert(conversation);
        return toSummaryResponse(conversation);
    }

    @Override
    public List<ConversationSummaryResponse> pageMerchantConversations() {
        Long merchantId = SecurityUtils.currentMerchantId();
        return conversationMapper.selectList(
            new LambdaQueryWrapper<CustomerConversation>()
                .eq(CustomerConversation::getMerchantId, merchantId)
                .orderByDesc(CustomerConversation::getLastMessageAt)
        ).stream().map(this::toSummaryResponse).toList();
    }

    @Override
    public ConversationSummaryResponse conversationDetail(Long conversationId) {
        return toSummaryResponse(requireMerchantConversation(conversationId));
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, String senderType) {
        CustomerConversation conversation = requireConversation(request.conversationId());
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        LoginUser currentUser = SecurityUtils.currentUser();
        if ("AI".equalsIgnoreCase(senderType)) {
            message.setSenderId(null);
            message.setSenderType("AI");
            message.setAiGenerated(1);
            message.setAiConfidence(new java.math.BigDecimal("0.8000"));
        } else {
            message.setSenderId(currentUser.getUserId());
            message.setSenderType(senderType.toUpperCase(Locale.ROOT));
            message.setAiGenerated(0);
        }
        message.setMessageType("TEXT");
        message.setContent(request.content());
        chatMessageMapper.insert(message);

        conversation.setLastMessage(request.content());
        conversation.setLastMessageAt(LocalDateTime.now());
        if ("CONSUMER".equalsIgnoreCase(senderType) && "AI_SERVING".equals(conversation.getStatus())) {
            conversation.setAiSummary(request.content());
        }
        conversationMapper.updateById(conversation);

        ChatMessageResponse response = toMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), response);

        if ("CONSUMER".equalsIgnoreCase(senderType) && "AI_SERVING".equals(conversation.getStatus())) {
            sendAiAck(conversation, request.content());
        }
        return response;
    }

    @Override
    @Transactional
    public ConversationSummaryResponse transferConversation(Long conversationId, TransferConversationRequest request) {
        CustomerConversation conversation = requireMerchantConversation(conversationId);
        conversation.setAssignedStaffId(request.staffId());
        conversation.setStatus("AGENT_SERVING");
        conversation.setTransferredAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return toSummaryResponse(conversation);
    }

    @Override
    @Transactional
    public void closeConversation(Long conversationId) {
        CustomerConversation conversation = requireMerchantConversation(conversationId);
        conversation.setStatus("CLOSED");
        conversation.setClosedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional
    public void evaluateConversation(Long conversationId, ServiceEvaluationRequest request) {
        LoginUser currentUser = SecurityUtils.currentUser();
        CustomerConversation conversation = requireConversation(conversationId);
        ServiceEvaluation evaluation = serviceEvaluationMapper.selectOne(
            new LambdaQueryWrapper<ServiceEvaluation>()
                .eq(ServiceEvaluation::getConversationId, conversationId)
                .last("limit 1")
        );
        if (evaluation == null) {
            evaluation = new ServiceEvaluation();
            evaluation.setConversationId(conversation.getId());
            evaluation.setUserId(currentUser.getUserId());
            evaluation.setMerchantId(conversation.getMerchantId());
            evaluation.setStaffId(conversation.getAssignedStaffId());
            evaluation.setRating(request.rating());
            evaluation.setComment(request.comment());
            serviceEvaluationMapper.insert(evaluation);
            return;
        }
        evaluation.setRating(request.rating());
        evaluation.setComment(request.comment());
        serviceEvaluationMapper.updateById(evaluation);
    }

    @Override
    public List<ChatMessageResponse> listMessages(Long conversationId) {
        requireMerchantConversation(conversationId);
        return chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreatedAt)
        ).stream().map(this::toMessageResponse).toList();
    }

    private CustomerConversation requireConversation(Long conversationId) {
        CustomerConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "Conversation not found");
        }
        return conversation;
    }

    private CustomerConversation requireMerchantConversation(Long conversationId) {
        CustomerConversation conversation = requireConversation(conversationId);
        Long merchantId = SecurityUtils.currentMerchantId();
        if (!merchantId.equals(conversation.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "Conversation not found");
        }
        return conversation;
    }

    private void sendAiAck(CustomerConversation conversation, String originalContent) {
        String replyText = buildAiReply(conversation, originalContent);
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setConversationId(conversation.getId());
        aiMessage.setSenderId(null);
        aiMessage.setSenderType("AI");
        aiMessage.setMessageType("TEXT");
        aiMessage.setContent(replyText);
        aiMessage.setAiGenerated(1);
        aiMessage.setAiConfidence(new java.math.BigDecimal("0.8600"));
        chatMessageMapper.insert(aiMessage);

        conversation.setLastMessage(replyText);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setAiSummary(replyText);
        conversationMapper.updateById(conversation);

        messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), toMessageResponse(aiMessage));
    }

    private String buildAiReply(CustomerConversation conversation, String originalContent) {
        try {
            ReplyResponse response = aiService.generateReply(new AiReplyRequest(
                originalContent,
                conversation.getMerchantId(),
                "CUSTOMER_CONVERSATION",
                conversation.getId(),
                null,
                conversation.getStatus(),
                null,
                String.valueOf(conversation.getId())
            ));
            if (response != null && response.reply() != null && !response.reply().isBlank()) {
                return response.reply();
            }
        } catch (Exception ignored) {
            // Keep the chat channel available if the external AI service is temporarily unavailable.
        }
        return "AI 服务暂不可用，建议为您转接人工客服继续处理。";
    }

    private ConversationSummaryResponse toSummaryResponse(CustomerConversation conversation) {
        return new ConversationSummaryResponse(
            conversation.getId(),
            conversation.getConversationNo(),
            conversation.getUserId(),
            conversation.getMerchantId(),
            conversation.getExternalOrderId(),
            conversation.getAssignedStaffId(),
            conversation.getSource(),
            conversation.getStatus(),
            conversation.getLastMessage(),
            conversation.getLastMessageAt(),
            conversation.getAiIntent(),
            conversation.getAiSummary()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
            message.getId(),
            message.getConversationId(),
            message.getSenderId(),
            message.getSenderType(),
            message.getMessageType(),
            message.getContent(),
            message.getMediaUrl(),
            message.getAiGenerated() != null && message.getAiGenerated() == 1,
            message.getAiConfidence(),
            message.getReadAt(),
            message.getCreatedAt()
        );
    }
}
