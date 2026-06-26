package com.example.mall.module.ai.service.impl;

import com.example.mall.module.ai.dto.AiReplyRequest;
import com.example.mall.module.ai.dto.AiTextRequest;
import com.example.mall.module.ai.dto.IntentResponse;
import com.example.mall.module.ai.dto.ReplyResponse;
import com.example.mall.module.ai.dto.SentimentResponse;
import com.example.mall.module.ai.dto.TicketClassifyResponse;
import com.example.mall.module.ai.dto.TopicResponse;
import com.example.mall.module.ai.service.AiService;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiServiceImpl implements AiService {

    private final WebClient aiWebClient;

    public AiServiceImpl(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    @Override
    public IntentResponse detectIntent(AiTextRequest request) {
        return post("/api/ai/intent", request, IntentResponse.class);
    }

    @Override
    public SentimentResponse analyzeSentiment(AiTextRequest request) {
        return post("/api/ai/sentiment", request, SentimentResponse.class);
    }

    @Override
    public TopicResponse extractTopic(AiTextRequest request) {
        return post("/api/ai/topic", request, TopicResponse.class);
    }

    @Override
    public TicketClassifyResponse classifyTicket(AiTextRequest request) {
        return post("/api/ai/ticket/classify", request, TicketClassifyResponse.class);
    }

    @Override
    public ReplyResponse generateReply(AiReplyRequest request) {
        return post("/api/ai/reply", request, ReplyResponse.class);
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        return aiWebClient.post()
            .uri(uri)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(responseType)
            .block(Duration.ofSeconds(5));
    }
}
