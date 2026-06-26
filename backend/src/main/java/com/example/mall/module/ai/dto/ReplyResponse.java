package com.example.mall.module.ai.dto;

import java.util.List;

public record ReplyResponse(String reply, String intent, Double confidence, List<String> suggestions) {
}
