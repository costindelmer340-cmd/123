package com.example.mall.module.ai.service;

import com.example.mall.module.ai.dto.AiReplyRequest;
import com.example.mall.module.ai.dto.AiTextRequest;
import com.example.mall.module.ai.dto.IntentResponse;
import com.example.mall.module.ai.dto.ReplyResponse;
import com.example.mall.module.ai.dto.ReviewAnalysisResponse;
import com.example.mall.module.ai.dto.SentimentResponse;
import com.example.mall.module.ai.dto.TicketClassifyResponse;
import com.example.mall.module.ai.dto.TopicResponse;

public interface AiService {

    IntentResponse detectIntent(AiTextRequest request);

    SentimentResponse analyzeSentiment(AiTextRequest request);

    TopicResponse extractTopic(AiTextRequest request);

    TicketClassifyResponse classifyTicket(AiTextRequest request);

    ReplyResponse generateReply(AiReplyRequest request);

    ReviewAnalysisResponse analyzeReview(AiTextRequest request);
}
