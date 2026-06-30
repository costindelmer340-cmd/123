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
import java.util.Map;
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

    @SuppressWarnings("unchecked")
    private <T> T post(String uri, Object body, Class<T> responseType) {
        try {
            Map<String, Object> response = aiWebClient.post()
                .uri(uri)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(10));
            
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (responseType == ReplyResponse.class) {
                    String reply = (String) data.get("reply");
                    String intent = (String) data.get("intent");
                    Double confidence = data.get("confidence") != null ? ((Number) data.get("confidence")).doubleValue() : null;
                    return (T) new ReplyResponse(reply, intent, confidence, null);
                } else if (responseType == IntentResponse.class) {
                    String intent = (String) data.get("intent");
                    String category = (String) data.get("category");
                    Double confidence = data.get("confidence") != null ? ((Number) data.get("confidence")).doubleValue() : null;
                    String summary = (String) data.get("summary");
                    return (T) new IntentResponse(intent, category, confidence, summary);
                } else if (responseType == SentimentResponse.class) {
                    String sentiment = (String) data.get("sentiment");
                    Double score = data.get("score") != null ? ((Number) data.get("score")).doubleValue() : null;
                    String riskLevel = (String) data.get("riskLevel");
                    String summary = (String) data.get("summary");
                    return (T) new SentimentResponse(sentiment, score, riskLevel, summary);
                } else if (responseType == TopicResponse.class) {
                    return (T) new TopicResponse(null, null, null);
                } else if (responseType == TicketClassifyResponse.class) {
                    String ticketType = (String) data.get("ticketType");
                    String priority = (String) data.get("priority");
                    String category = (String) data.get("category");
                    Double confidence = data.get("confidence") != null ? ((Number) data.get("confidence")).doubleValue() : null;
                    Integer dueHours = data.get("dueHours") != null ? ((Number) data.get("dueHours")).intValue() : null;
                    return (T) new TicketClassifyResponse(ticketType, priority, category, confidence, dueHours);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}