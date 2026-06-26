package com.example.mall.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeArticleRequest(
    @NotBlank(message = "标题不能为空")
    String title,
    @NotBlank(message = "内容不能为空")
    String content,
    @NotBlank(message = "分类不能为空")
    String category,
    String tags,
    String status
) {
}
