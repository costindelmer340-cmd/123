package com.example.mall.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.auth.SecurityUtils;
import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.knowledge.dto.AfterSaleRuleRequest;
import com.example.mall.module.knowledge.dto.AfterSaleRuleResponse;
import com.example.mall.module.knowledge.dto.FaqItemRequest;
import com.example.mall.module.knowledge.dto.FaqItemResponse;
import com.example.mall.module.knowledge.dto.KnowledgeArticleRequest;
import com.example.mall.module.knowledge.dto.KnowledgeArticleResponse;
import com.example.mall.module.knowledge.entity.AfterSaleRule;
import com.example.mall.module.knowledge.entity.FaqItem;
import com.example.mall.module.knowledge.entity.KnowledgeArticle;
import com.example.mall.module.knowledge.mapper.AfterSaleRuleMapper;
import com.example.mall.module.knowledge.mapper.FaqItemMapper;
import com.example.mall.module.knowledge.mapper.KnowledgeArticleMapper;
import com.example.mall.module.knowledge.service.KnowledgeService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final FaqItemMapper faqItemMapper;
    private final AfterSaleRuleMapper afterSaleRuleMapper;

    public KnowledgeServiceImpl(
        KnowledgeArticleMapper knowledgeArticleMapper,
        FaqItemMapper faqItemMapper,
        AfterSaleRuleMapper afterSaleRuleMapper
    ) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.faqItemMapper = faqItemMapper;
        this.afterSaleRuleMapper = afterSaleRuleMapper;
    }

    @Override
    public PageResponse<KnowledgeArticleResponse> pageArticles(long pageNum, long pageSize, String category, String keyword) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Page<KnowledgeArticle> page = knowledgeArticleMapper.selectPage(
            new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getMerchantId, merchantId)
                .eq(category != null && !category.isBlank(), KnowledgeArticle::getCategory, category)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                    .like(KnowledgeArticle::getTitle, keyword)
                    .or()
                    .like(KnowledgeArticle::getContent, keyword)
                )
                .orderByDesc(KnowledgeArticle::getCreatedAt)
        );
        List<KnowledgeArticleResponse> records = page.getRecords().stream().map(this::toArticleResponse).toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    @Transactional
    public KnowledgeArticleResponse createArticle(KnowledgeArticleRequest request) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setMerchantId(SecurityUtils.currentMerchantId());
        article.setTitle(request.title());
        article.setContent(request.content());
        article.setCategory(request.category());
        article.setTags(request.tags());
        article.setStatus(request.status() == null || request.status().isBlank() ? "PUBLISHED" : request.status());
        article.setCreatedBy(SecurityUtils.currentUser().getUserId());
        knowledgeArticleMapper.insert(article);
        return toArticleResponse(article);
    }

    @Override
    @Transactional
    public KnowledgeArticleResponse updateArticle(Long articleId, KnowledgeArticleRequest request) {
        KnowledgeArticle article = requireArticle(articleId);
        article.setTitle(request.title());
        article.setContent(request.content());
        article.setCategory(request.category());
        article.setTags(request.tags());
        article.setStatus(request.status() == null || request.status().isBlank() ? article.getStatus() : request.status());
        knowledgeArticleMapper.updateById(article);
        return toArticleResponse(article);
    }

    @Override
    public void deleteArticle(Long articleId) {
        requireArticle(articleId);
        knowledgeArticleMapper.deleteById(articleId);
    }

    @Override
    public PageResponse<FaqItemResponse> pageFaqs(long pageNum, long pageSize, String category, String keyword) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Page<FaqItem> page = faqItemMapper.selectPage(
            new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
            new LambdaQueryWrapper<FaqItem>()
                .eq(FaqItem::getMerchantId, merchantId)
                .eq(category != null && !category.isBlank(), FaqItem::getCategory, category)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                    .like(FaqItem::getQuestion, keyword)
                    .or()
                    .like(FaqItem::getAnswer, keyword)
                )
                .orderByDesc(FaqItem::getPriority)
                .orderByDesc(FaqItem::getCreatedAt)
        );
        List<FaqItemResponse> records = page.getRecords().stream().map(this::toFaqResponse).toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    @Transactional
    public FaqItemResponse createFaq(FaqItemRequest request) {
        FaqItem faq = new FaqItem();
        faq.setMerchantId(SecurityUtils.currentMerchantId());
        faq.setQuestion(request.question());
        faq.setAnswer(request.answer());
        faq.setCategory(request.category());
        faq.setPriority(request.priority() == null ? 0 : request.priority());
        faq.setEnabled(request.enabled() == null || request.enabled() ? 1 : 0);
        faq.setCreatedBy(SecurityUtils.currentUser().getUserId());
        faqItemMapper.insert(faq);
        return toFaqResponse(faq);
    }

    @Override
    @Transactional
    public FaqItemResponse updateFaq(Long faqId, FaqItemRequest request) {
        FaqItem faq = requireFaq(faqId);
        faq.setQuestion(request.question());
        faq.setAnswer(request.answer());
        faq.setCategory(request.category());
        faq.setPriority(request.priority() == null ? faq.getPriority() : request.priority());
        faq.setEnabled(request.enabled() == null ? faq.getEnabled() : (request.enabled() ? 1 : 0));
        faqItemMapper.updateById(faq);
        return toFaqResponse(faq);
    }

    @Override
    public void deleteFaq(Long faqId) {
        requireFaq(faqId);
        faqItemMapper.deleteById(faqId);
    }

    @Override
    public PageResponse<AfterSaleRuleResponse> pageRules(long pageNum, long pageSize, String ruleType, String keyword) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Page<AfterSaleRule> page = afterSaleRuleMapper.selectPage(
            new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
            new LambdaQueryWrapper<AfterSaleRule>()
                .eq(AfterSaleRule::getMerchantId, merchantId)
                .eq(ruleType != null && !ruleType.isBlank(), AfterSaleRule::getRuleType, ruleType)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                    .like(AfterSaleRule::getRuleName, keyword)
                    .or()
                    .like(AfterSaleRule::getContent, keyword)
                )
                .orderByDesc(AfterSaleRule::getCreatedAt)
        );
        List<AfterSaleRuleResponse> records = page.getRecords().stream().map(this::toRuleResponse).toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    @Transactional
    public AfterSaleRuleResponse createRule(AfterSaleRuleRequest request) {
        AfterSaleRule rule = new AfterSaleRule();
        rule.setMerchantId(SecurityUtils.currentMerchantId());
        rule.setRuleName(request.ruleName());
        rule.setRuleType(request.ruleType());
        rule.setConditionsJson(request.conditionsJson());
        rule.setActionJson(request.actionJson());
        rule.setContent(request.content());
        rule.setEnabled(request.enabled() == null || request.enabled() ? 1 : 0);
        rule.setCreatedBy(SecurityUtils.currentUser().getUserId());
        afterSaleRuleMapper.insert(rule);
        return toRuleResponse(rule);
    }

    @Override
    @Transactional
    public AfterSaleRuleResponse updateRule(Long ruleId, AfterSaleRuleRequest request) {
        AfterSaleRule rule = requireRule(ruleId);
        rule.setRuleName(request.ruleName());
        rule.setRuleType(request.ruleType());
        rule.setConditionsJson(request.conditionsJson());
        rule.setActionJson(request.actionJson());
        rule.setContent(request.content());
        rule.setEnabled(request.enabled() == null ? rule.getEnabled() : (request.enabled() ? 1 : 0));
        afterSaleRuleMapper.updateById(rule);
        return toRuleResponse(rule);
    }

    @Override
    public void deleteRule(Long ruleId) {
        requireRule(ruleId);
        afterSaleRuleMapper.deleteById(ruleId);
    }

    private KnowledgeArticle requireArticle(Long id) {
        Long merchantId = SecurityUtils.currentMerchantId();
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null || !merchantId.equals(article.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "Knowledge article not found");
        }
        return article;
    }

    private FaqItem requireFaq(Long id) {
        Long merchantId = SecurityUtils.currentMerchantId();
        FaqItem faq = faqItemMapper.selectById(id);
        if (faq == null || !merchantId.equals(faq.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "FAQ not found");
        }
        return faq;
    }

    private AfterSaleRule requireRule(Long id) {
        Long merchantId = SecurityUtils.currentMerchantId();
        AfterSaleRule rule = afterSaleRuleMapper.selectById(id);
        if (rule == null || !merchantId.equals(rule.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "After-sale rule not found");
        }
        return rule;
    }

    private KnowledgeArticleResponse toArticleResponse(KnowledgeArticle article) {
        return new KnowledgeArticleResponse(article.getId(), article.getMerchantId(), article.getTitle(), article.getContent(), article.getCategory(), article.getTags(), article.getStatus(), article.getCreatedBy(), article.getCreatedAt());
    }

    private FaqItemResponse toFaqResponse(FaqItem faq) {
        return new FaqItemResponse(faq.getId(), faq.getMerchantId(), faq.getQuestion(), faq.getAnswer(), faq.getCategory(), faq.getPriority(), faq.getEnabled() != null && faq.getEnabled() == 1, faq.getCreatedBy(), faq.getCreatedAt());
    }

    private AfterSaleRuleResponse toRuleResponse(AfterSaleRule rule) {
        return new AfterSaleRuleResponse(rule.getId(), rule.getMerchantId(), rule.getRuleName(), rule.getRuleType(), rule.getConditionsJson(), rule.getActionJson(), rule.getContent(), rule.getEnabled() != null && rule.getEnabled() == 1, rule.getCreatedBy(), rule.getCreatedAt());
    }
}
