package com.example.mall.module.knowledge.controller;

import com.example.mall.common.response.ApiResponse;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.knowledge.dto.AfterSaleRuleRequest;
import com.example.mall.module.knowledge.dto.AfterSaleRuleResponse;
import com.example.mall.module.knowledge.dto.FaqItemRequest;
import com.example.mall.module.knowledge.dto.FaqItemResponse;
import com.example.mall.module.knowledge.dto.KnowledgeArticleRequest;
import com.example.mall.module.knowledge.dto.KnowledgeArticleResponse;
import com.example.mall.module.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/knowledge")
public class MerchantKnowledgeController {

    private final KnowledgeService knowledgeService;

    public MerchantKnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/articles")
    public ApiResponse<PageResponse<KnowledgeArticleResponse>> articles(@RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String category, @RequestParam(required = false) String keyword) {
        return ApiResponse.success(knowledgeService.pageArticles(pageNum, pageSize, category, keyword), traceId());
    }

    @PostMapping("/articles")
    public ApiResponse<KnowledgeArticleResponse> createArticle(@Valid @RequestBody KnowledgeArticleRequest request) {
        return ApiResponse.success(knowledgeService.createArticle(request), traceId());
    }

    @PutMapping("/articles/{articleId}")
    public ApiResponse<KnowledgeArticleResponse> updateArticle(@PathVariable Long articleId, @Valid @RequestBody KnowledgeArticleRequest request) {
        return ApiResponse.success(knowledgeService.updateArticle(articleId, request), traceId());
    }

    @DeleteMapping("/articles/{articleId}")
    public ApiResponse<Void> deleteArticle(@PathVariable Long articleId) {
        knowledgeService.deleteArticle(articleId);
        return ApiResponse.success(traceId());
    }

    @GetMapping("/faqs")
    public ApiResponse<PageResponse<FaqItemResponse>> faqs(@RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String category, @RequestParam(required = false) String keyword) {
        return ApiResponse.success(knowledgeService.pageFaqs(pageNum, pageSize, category, keyword), traceId());
    }

    @PostMapping("/faqs")
    public ApiResponse<FaqItemResponse> createFaq(@Valid @RequestBody FaqItemRequest request) {
        return ApiResponse.success(knowledgeService.createFaq(request), traceId());
    }

    @PutMapping("/faqs/{faqId}")
    public ApiResponse<FaqItemResponse> updateFaq(@PathVariable Long faqId, @Valid @RequestBody FaqItemRequest request) {
        return ApiResponse.success(knowledgeService.updateFaq(faqId, request), traceId());
    }

    @DeleteMapping("/faqs/{faqId}")
    public ApiResponse<Void> deleteFaq(@PathVariable Long faqId) {
        knowledgeService.deleteFaq(faqId);
        return ApiResponse.success(traceId());
    }

    @GetMapping("/rules")
    public ApiResponse<PageResponse<AfterSaleRuleResponse>> rules(@RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String ruleType, @RequestParam(required = false) String keyword) {
        return ApiResponse.success(knowledgeService.pageRules(pageNum, pageSize, ruleType, keyword), traceId());
    }

    @PostMapping("/rules")
    public ApiResponse<AfterSaleRuleResponse> createRule(@Valid @RequestBody AfterSaleRuleRequest request) {
        return ApiResponse.success(knowledgeService.createRule(request), traceId());
    }

    @PutMapping("/rules/{ruleId}")
    public ApiResponse<AfterSaleRuleResponse> updateRule(@PathVariable Long ruleId, @Valid @RequestBody AfterSaleRuleRequest request) {
        return ApiResponse.success(knowledgeService.updateRule(ruleId, request), traceId());
    }

    @DeleteMapping("/rules/{ruleId}")
    public ApiResponse<Void> deleteRule(@PathVariable Long ruleId) {
        knowledgeService.deleteRule(ruleId);
        return ApiResponse.success(traceId());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
