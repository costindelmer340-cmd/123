package com.example.mall.module.knowledge.service;

import com.example.mall.common.response.PageResponse;
import com.example.mall.module.knowledge.dto.AfterSaleRuleRequest;
import com.example.mall.module.knowledge.dto.AfterSaleRuleResponse;
import com.example.mall.module.knowledge.dto.FaqItemRequest;
import com.example.mall.module.knowledge.dto.FaqItemResponse;
import com.example.mall.module.knowledge.dto.KnowledgeArticleRequest;
import com.example.mall.module.knowledge.dto.KnowledgeArticleResponse;

public interface KnowledgeService {

    PageResponse<KnowledgeArticleResponse> pageArticles(long pageNum, long pageSize, String category, String keyword);

    KnowledgeArticleResponse createArticle(KnowledgeArticleRequest request);

    KnowledgeArticleResponse updateArticle(Long articleId, KnowledgeArticleRequest request);

    void deleteArticle(Long articleId);

    PageResponse<FaqItemResponse> pageFaqs(long pageNum, long pageSize, String category, String keyword);

    FaqItemResponse createFaq(FaqItemRequest request);

    FaqItemResponse updateFaq(Long faqId, FaqItemRequest request);

    void deleteFaq(Long faqId);

    PageResponse<AfterSaleRuleResponse> pageRules(long pageNum, long pageSize, String ruleType, String keyword);

    AfterSaleRuleResponse createRule(AfterSaleRuleRequest request);

    AfterSaleRuleResponse updateRule(Long ruleId, AfterSaleRuleRequest request);

    void deleteRule(Long ruleId);
}
