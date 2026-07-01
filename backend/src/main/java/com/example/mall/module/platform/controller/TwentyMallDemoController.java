package com.example.mall.module.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mall.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.mall.module.ai.dto.AiTextRequest;
import com.example.mall.module.ai.dto.ReviewAnalysisResponse;
import com.example.mall.module.ai.service.AiService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/twenty-mall")
public class TwentyMallDemoController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AiService aiService;
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.M.d HH:mm:ss");
    private static final Path EVIDENCE_DIR = Paths.get("target", "demo-evidence");

    public TwentyMallDemoController(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, AiService aiService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.aiService = aiService;
    }

    @GetMapping("/admin/overview")
    public ApiResponse<TwentyMallAdminOverviewResponse> adminOverview() {
        Long merchantCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM twenty_mall_account
            WHERE account_role = 'MERCHANT' AND status = 'ACTIVE' AND deleted = 0
            """,
            Long.class
        );
        Long boundShopCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(DISTINCT b.secondary_account_no)
            FROM platform_account_binding b
            JOIN primary_account pa ON pa.id = b.primary_account_id
            JOIN twenty_mall_account tm ON tm.account_no = b.secondary_account_no
              AND tm.account_role = b.secondary_account_role
              AND tm.deleted = 0
              AND tm.status = 'ACTIVE'
            WHERE b.platform_code = 'TWENTY_MALL'
              AND b.secondary_account_role = 'MERCHANT'
              AND b.bind_status = 'BOUND'
              AND b.deleted = 0
              AND pa.account_type = 'MERCHANT'
              AND pa.deleted = 0
            """,
            Long.class
        );
        Long todaySyncCount = jdbcTemplate.queryForObject(
            """
            SELECT (
              SELECT COUNT(*) FROM twenty_mall_order WHERE deleted = 0 AND DATE(updated_at) = CURDATE()
            ) + (
              SELECT COUNT(*) FROM twenty_mall_after_sale WHERE deleted = 0 AND DATE(updated_at) = CURDATE()
            ) + (
              SELECT COUNT(*) FROM twenty_mall_review WHERE deleted = 0 AND DATE(updated_at) = CURDATE()
            )
            """,
            Long.class
        );
        Long pendingAfterSaleCount = countLong(
            "SELECT COUNT(*) FROM twenty_mall_after_sale WHERE deleted = 0 AND status = 'PENDING_REVIEW'"
        );
        Long processingAfterSaleCount = countLong(
            "SELECT COUNT(*) FROM twenty_mall_after_sale WHERE deleted = 0 AND status = 'PROCESSING'"
        );
        Long highRiskReviewCount = countLong(
            """
            SELECT COUNT(*)
            FROM twenty_mall_review
            WHERE deleted = 0
              AND (product_score <= 2 OR service_score <= 2 OR content LIKE '%质量问题%' OR content LIKE '%破损%' OR content LIKE '%划痕%')
            """
        );
        Long activeRuleCount = countLong("SELECT COUNT(*) FROM after_sale_rule WHERE deleted = 0 AND enabled = 1");
        Long knowledgeCount = countLong(
            """
            SELECT (
              SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND status = 'PUBLISHED'
            ) + (
              SELECT COUNT(*) FROM faq_item WHERE deleted = 0 AND enabled = 1
            )
            """
        );
        return ApiResponse.success(new TwentyMallAdminOverviewResponse(
            merchantCount == null ? 0 : merchantCount,
            boundShopCount == null ? 0 : boundShopCount,
            todaySyncCount == null ? 0 : todaySyncCount,
            pendingAfterSaleCount,
            processingAfterSaleCount,
            highRiskReviewCount,
            activeRuleCount,
            knowledgeCount,
            loadAdminTrendRows(),
            loadAdminActivityRows()
        ), traceId());
    }

    @GetMapping("/admin/account-bindings")
    public ApiResponse<TwentyMallAdminBindingOverviewResponse> adminAccountBindings() {
        return ApiResponse.success(new TwentyMallAdminBindingOverviewResponse(
            loadAdminBindingRows("CONSUMER"),
            loadAdminBindingRows("MERCHANT")
        ), traceId());
    }

    @GetMapping("/admin/sync-monitor")
    public ApiResponse<List<TwentyMallAdminSyncLogResponse>> adminSyncMonitor() {
        return ApiResponse.success(List.of(
            buildSyncLog(
                "20商城订单数据同步",
                "SELECT COUNT(*) FROM twenty_mall_order WHERE deleted = 0",
                "SELECT MAX(updated_at) FROM twenty_mall_order WHERE deleted = 0"
            ),
            buildSyncLog(
                "20商城售后数据同步",
                "SELECT COUNT(*) FROM twenty_mall_after_sale WHERE deleted = 0",
                "SELECT MAX(updated_at) FROM twenty_mall_after_sale WHERE deleted = 0"
            ),
            buildSyncLog(
                "20商城评价数据同步",
                "SELECT COUNT(*) FROM twenty_mall_review WHERE deleted = 0",
                "SELECT MAX(updated_at) FROM twenty_mall_review WHERE deleted = 0"
            )
        ), traceId());
    }

    @GetMapping("/admin/reviews")
    public ApiResponse<List<TwentyMallAdminReviewResponse>> adminReviews() {
        ensureReviewDisputeTable();
        String sql = """
            SELECT r.id, r.product_score, r.service_score, r.content, r.reviewed_at, r.created_at,
                   o.order_no, p.product_name, ma.display_name AS merchant_name,
                   d.id AS dispute_id, d.status AS dispute_status, d.reason AS dispute_reason,
                   d.admin_note AS dispute_admin_note, d.created_at AS dispute_created_at
            FROM twenty_mall_review r
            JOIN twenty_mall_order o ON o.id = r.order_id
            JOIN twenty_mall_product p ON p.id = r.product_id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            LEFT JOIN twenty_mall_review_dispute d ON d.review_id = r.id AND d.deleted = 0
            WHERE r.deleted = 0 AND o.deleted = 0
            ORDER BY COALESCE(r.reviewed_at, r.created_at) DESC, r.id DESC
            """;
        return ApiResponse.success(jdbcTemplate.query(sql, (rs, rowNum) -> {
            String content = rs.getString("content");
            int productScore = rs.getInt("product_score");
            int serviceScore = rs.getInt("service_score");
            String sentiment = reviewSentiment(productScore, serviceScore, content);
            String riskLevel = reviewRiskLevel(productScore, serviceScore, content);
            return new TwentyMallAdminReviewResponse(
                rs.getLong("id"),
                "20商城",
                rs.getString("order_no"),
                rs.getString("merchant_name"),
                cleanProductName(rs.getString("product_name")),
                productScore,
                serviceScore,
                Math.round((productScore + serviceScore) / 2.0f),
                content,
                sentimentText(sentiment),
                riskLevelText(riskLevel),
                reviewKeywords(content, productScore),
                reviewSummary(sentiment, riskLevel, content),
                reviewSuggestion(riskLevel),
                formatTime(rs.getTimestamp("reviewed_at") == null ? rs.getTimestamp("created_at") : rs.getTimestamp("reviewed_at")),
                rs.getLong("dispute_id") == 0 ? null : rs.getLong("dispute_id"),
                disputeStatusText(rs.getString("dispute_status")),
                rs.getString("dispute_reason"),
                rs.getString("dispute_admin_note"),
                formatTime(rs.getTimestamp("dispute_created_at"))
            );
        }), traceId());
    }

    @PostMapping("/admin/reviews/{reviewId}/delete")
    public ApiResponse<Map<String, Object>> deleteAdminReview(@PathVariable Long reviewId) {
        int affected = jdbcTemplate.update(
            "UPDATE twenty_mall_review SET deleted = 1, updated_at = NOW() WHERE id = ? AND deleted = 0",
            reviewId
        );
        if (affected == 0) {
            return ApiResponse.fail("404", "评价不存在或已删除", traceId());
        }
        return ApiResponse.success(Map.of("reviewId", reviewId, "deleted", true), traceId());
    }

    @PostMapping("/admin/reviews/disputes/{disputeId}/review")
    public ApiResponse<Map<String, Object>> reviewAdminReviewDispute(
        @PathVariable Long disputeId,
        @RequestBody TwentyMallReviewDisputeReviewRequest request
    ) {
        ensureReviewDisputeTable();
        String result = request.result() == null ? "" : request.result().trim().toUpperCase();
        if (!"APPROVE".equals(result) && !"REJECT".equals(result)) {
            return ApiResponse.fail("400", "审核结果只能为 APPROVE 或 REJECT", traceId());
        }
        List<Long> reviewIds = jdbcTemplate.query(
            "SELECT review_id FROM twenty_mall_review_dispute WHERE id = ? AND deleted = 0 AND status = 'PENDING' LIMIT 1",
            (rs, rowNum) -> rs.getLong("review_id"),
            disputeId
        );
        if (reviewIds.isEmpty()) {
            return ApiResponse.fail("404", "异议不存在或已处理", traceId());
        }
        String nextStatus = "APPROVE".equals(result) ? "APPROVED" : "REJECTED";
        jdbcTemplate.update(
            """
            UPDATE twenty_mall_review_dispute
            SET status = ?, admin_note = ?, reviewed_at = NOW(), updated_at = NOW()
            WHERE id = ? AND deleted = 0
            """,
            nextStatus,
            request.adminNote(),
            disputeId
        );
        if ("APPROVED".equals(nextStatus)) {
            jdbcTemplate.update(
                "UPDATE twenty_mall_review SET deleted = 1, updated_at = NOW() WHERE id = ? AND deleted = 0",
                reviewIds.get(0)
            );
        }
        return ApiResponse.success(Map.of("disputeId", disputeId, "status", nextStatus), traceId());
    }

    @GetMapping("/admin/rules")
    public ApiResponse<List<TwentyMallAdminRuleResponse>> adminRules() {
        String sql = """
            SELECT id, rule_name, rule_type, conditions_json, action_json, content, enabled, updated_at
            FROM after_sale_rule
            WHERE deleted = 0
            ORDER BY enabled DESC, updated_at DESC, id DESC
            """;
        return ApiResponse.success(jdbcTemplate.query(sql, (rs, rowNum) -> new TwentyMallAdminRuleResponse(
            rs.getLong("id"),
            rs.getString("rule_name"),
            rs.getString("rule_type"),
            ruleTypeText(rs.getString("rule_type")),
            rs.getString("conditions_json"),
            ruleConditionText(rs.getString("conditions_json")),
            rs.getString("action_json"),
            ruleActionText(rs.getString("action_json")),
            rs.getString("content"),
            rs.getBoolean("enabled"),
            formatTime(rs.getTimestamp("updated_at"))
        )), traceId());
    }

    @PostMapping("/admin/rules")
    public ApiResponse<TwentyMallAdminRuleResponse> saveAdminRule(@Valid @RequestBody TwentyMallAdminRuleSaveRequest request) {
        String conditionsJson = normalizedJson(request.conditionsJson());
        String actionJson = normalizedJson(request.actionJson());
        if (conditionsJson == null || actionJson == null) {
            return ApiResponse.fail("400", "规则条件和执行动作必须是合法 JSON", traceId());
        }
        if (request.id() == null) {
            jdbcTemplate.update(
                """
                INSERT INTO after_sale_rule (merchant_id, rule_name, rule_type, conditions_json, action_json, content, enabled)
                VALUES (NULL, ?, ?, ?, ?, ?, ?)
                """,
                request.ruleName(),
                request.ruleType(),
                conditionsJson,
                actionJson,
                request.content(),
                Boolean.TRUE.equals(request.enabled()) ? 1 : 0
            );
            Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            return adminRuleById(id == null ? 0L : id);
        }
        int affected = jdbcTemplate.update(
            """
            UPDATE after_sale_rule
            SET rule_name = ?, rule_type = ?, conditions_json = ?, action_json = ?,
                content = ?, enabled = ?, updated_at = NOW()
            WHERE id = ? AND deleted = 0
            """,
            request.ruleName(),
            request.ruleType(),
            conditionsJson,
            actionJson,
            request.content(),
            Boolean.TRUE.equals(request.enabled()) ? 1 : 0,
            request.id()
        );
        if (affected == 0) {
            return ApiResponse.fail("404", "规则不存在或已删除", traceId());
        }
        return adminRuleById(request.id());
    }

    @PostMapping("/admin/rules/{ruleId}/toggle")
    public ApiResponse<Map<String, Object>> toggleAdminRule(
        @PathVariable Long ruleId,
        @RequestBody TwentyMallAdminRuleToggleRequest request
    ) {
        int affected = jdbcTemplate.update(
            "UPDATE after_sale_rule SET enabled = ?, updated_at = NOW() WHERE id = ? AND deleted = 0",
            Boolean.TRUE.equals(request.enabled()) ? 1 : 0,
            ruleId
        );
        if (affected == 0) {
            return ApiResponse.fail("404", "规则不存在或已删除", traceId());
        }
        return ApiResponse.success(Map.of("ruleId", ruleId, "enabled", Boolean.TRUE.equals(request.enabled())), traceId());
    }

    @PostMapping("/admin/rules/{ruleId}/delete")
    public ApiResponse<Map<String, Object>> deleteAdminRule(@PathVariable Long ruleId) {
        int affected = jdbcTemplate.update(
            "UPDATE after_sale_rule SET deleted = 1, updated_at = NOW() WHERE id = ? AND deleted = 0",
            ruleId
        );
        if (affected == 0) {
            return ApiResponse.fail("404", "规则不存在或已删除", traceId());
        }
        return ApiResponse.success(Map.of("ruleId", ruleId, "deleted", true), traceId());
    }

    @GetMapping("/admin/knowledge")
    public ApiResponse<TwentyMallAdminKnowledgeOverviewResponse> adminKnowledge() {
        List<TwentyMallAdminKnowledgeArticleResponse> articles = jdbcTemplate.query(
            """
            SELECT id, title, content, category, tags, status, updated_at
            FROM knowledge_article
            WHERE deleted = 0
            ORDER BY updated_at DESC, id DESC
            """,
            (rs, rowNum) -> new TwentyMallAdminKnowledgeArticleResponse(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("category"),
                knowledgeCategoryText(rs.getString("category")),
                rs.getString("tags"),
                rs.getString("status"),
                knowledgeStatusText(rs.getString("status")),
                formatTime(rs.getTimestamp("updated_at"))
            )
        );
        List<TwentyMallAdminFaqResponse> faqs = jdbcTemplate.query(
            """
            SELECT id, question, answer, category, priority, enabled, updated_at
            FROM faq_item
            WHERE deleted = 0
            ORDER BY enabled DESC, priority DESC, updated_at DESC, id DESC
            """,
            (rs, rowNum) -> new TwentyMallAdminFaqResponse(
                rs.getLong("id"),
                rs.getString("question"),
                rs.getString("answer"),
                rs.getString("category"),
                faqCategoryText(rs.getString("category")),
                rs.getInt("priority"),
                rs.getBoolean("enabled"),
                formatTime(rs.getTimestamp("updated_at"))
            )
        );
        return ApiResponse.success(new TwentyMallAdminKnowledgeOverviewResponse(articles, faqs), traceId());
    }

    @PostMapping("/admin/knowledge/articles")
    public ApiResponse<Map<String, Object>> saveKnowledgeArticle(@Valid @RequestBody TwentyMallAdminKnowledgeArticleSaveRequest request) {
        String tagsJson = normalizedJson(request.tagsJson());
        if (tagsJson == null) {
            return ApiResponse.fail("400", "标签必须是合法 JSON 数组", traceId());
        }
        if (request.id() == null) {
            jdbcTemplate.update(
                "INSERT INTO knowledge_article (title, content, category, tags, status) VALUES (?, ?, ?, ?, ?)",
                request.title(),
                request.content(),
                request.category(),
                tagsJson,
                request.status()
            );
        } else {
            int affected = jdbcTemplate.update(
                """
                UPDATE knowledge_article
                SET title = ?, content = ?, category = ?, tags = ?, status = ?, updated_at = NOW()
                WHERE id = ? AND deleted = 0
                """,
                request.title(),
                request.content(),
                request.category(),
                tagsJson,
                request.status(),
                request.id()
            );
            if (affected == 0) {
                return ApiResponse.fail("404", "知识文章不存在或已删除", traceId());
            }
        }
        return ApiResponse.success(Map.of("saved", true), traceId());
    }

    @PostMapping("/admin/knowledge/articles/{articleId}/toggle")
    public ApiResponse<Map<String, Object>> toggleKnowledgeArticle(@PathVariable Long articleId, @RequestBody TwentyMallAdminKnowledgeToggleRequest request) {
        String status = Boolean.TRUE.equals(request.enabled()) ? "PUBLISHED" : "DRAFT";
        int affected = jdbcTemplate.update(
            "UPDATE knowledge_article SET status = ?, updated_at = NOW() WHERE id = ? AND deleted = 0",
            status,
            articleId
        );
        if (affected == 0) {
            return ApiResponse.fail("404", "知识文章不存在或已删除", traceId());
        }
        return ApiResponse.success(Map.of("articleId", articleId, "status", status), traceId());
    }

    @PostMapping("/admin/knowledge/articles/{articleId}/delete")
    public ApiResponse<Map<String, Object>> deleteKnowledgeArticle(@PathVariable Long articleId) {
        int affected = jdbcTemplate.update(
            "UPDATE knowledge_article SET deleted = 1, updated_at = NOW() WHERE id = ? AND deleted = 0",
            articleId
        );
        if (affected == 0) {
            return ApiResponse.fail("404", "知识文章不存在或已删除", traceId());
        }
        return ApiResponse.success(Map.of("articleId", articleId, "deleted", true), traceId());
    }

    @PostMapping("/admin/knowledge/faqs")
    public ApiResponse<Map<String, Object>> saveFaq(@Valid @RequestBody TwentyMallAdminFaqSaveRequest request) {
        if (request.id() == null) {
            jdbcTemplate.update(
                "INSERT INTO faq_item (question, answer, category, priority, enabled) VALUES (?, ?, ?, ?, ?)",
                request.question(),
                request.answer(),
                request.category(),
                request.priority() == null ? 0 : request.priority(),
                Boolean.TRUE.equals(request.enabled()) ? 1 : 0
            );
        } else {
            int affected = jdbcTemplate.update(
                """
                UPDATE faq_item
                SET question = ?, answer = ?, category = ?, priority = ?, enabled = ?, updated_at = NOW()
                WHERE id = ? AND deleted = 0
                """,
                request.question(),
                request.answer(),
                request.category(),
                request.priority() == null ? 0 : request.priority(),
                Boolean.TRUE.equals(request.enabled()) ? 1 : 0,
                request.id()
            );
            if (affected == 0) {
                return ApiResponse.fail("404", "常见问题不存在或已删除", traceId());
            }
        }
        return ApiResponse.success(Map.of("saved", true), traceId());
    }

    @PostMapping("/admin/knowledge/faqs/{faqId}/toggle")
    public ApiResponse<Map<String, Object>> toggleFaq(@PathVariable Long faqId, @RequestBody TwentyMallAdminKnowledgeToggleRequest request) {
        int affected = jdbcTemplate.update(
            "UPDATE faq_item SET enabled = ?, updated_at = NOW() WHERE id = ? AND deleted = 0",
            Boolean.TRUE.equals(request.enabled()) ? 1 : 0,
            faqId
        );
        if (affected == 0) {
            return ApiResponse.fail("404", "常见问题不存在或已删除", traceId());
        }
        return ApiResponse.success(Map.of("faqId", faqId, "enabled", Boolean.TRUE.equals(request.enabled())), traceId());
    }

    @PostMapping("/admin/knowledge/faqs/{faqId}/delete")
    public ApiResponse<Map<String, Object>> deleteFaq(@PathVariable Long faqId) {
        int affected = jdbcTemplate.update(
            "UPDATE faq_item SET deleted = 1, updated_at = NOW() WHERE id = ? AND deleted = 0",
            faqId
        );
        if (affected == 0) {
            return ApiResponse.fail("404", "常见问题不存在或已删除", traceId());
        }
        return ApiResponse.success(Map.of("faqId", faqId, "deleted", true), traceId());
    }

    @GetMapping("/primary/bindings")
    public ApiResponse<List<TwentyMallAdminBindingResponse>> primaryBindings(
        @RequestParam String primaryAccountNo,
        @RequestParam String primaryAccountType,
        @RequestParam(required = false) String secondaryAccountRole
    ) {
        String roleFilter = secondaryAccountRole == null || secondaryAccountRole.isBlank() ? primaryAccountType : secondaryAccountRole;
        String sql = """
            SELECT pa.account_no AS primary_account_no,
                   pa.display_name AS primary_display_name,
                   pa.avatar_url AS primary_avatar,
                   b.platform_name,
                   b.secondary_account_no,
                   b.secondary_account_role,
                   tm.display_name AS secondary_display_name,
                   b.bind_status,
                   tm.status,
                   b.bound_at
            FROM platform_account_binding b
            JOIN primary_account pa ON pa.id = b.primary_account_id
            LEFT JOIN twenty_mall_account tm ON tm.account_no = b.secondary_account_no
              AND tm.account_role = b.secondary_account_role
              AND tm.deleted = 0
            WHERE pa.account_no = ?
              AND pa.account_type = ?
              AND b.secondary_account_role = ?
              AND pa.deleted = 0
              AND b.deleted = 0
              AND b.bind_status = 'BOUND'
            ORDER BY b.platform_code, b.secondary_account_no
            """;
        return ApiResponse.success(jdbcTemplate.query(sql, (rs, rowNum) -> new TwentyMallAdminBindingResponse(
            rs.getString("primary_account_no"),
            rs.getString("primary_display_name"),
            normalizedPrimaryAvatar(rs.getString("primary_avatar")),
            rs.getString("platform_name"),
            rs.getString("secondary_account_no"),
            rs.getString("secondary_display_name"),
            bindStatusText(rs.getString("bind_status")),
            rs.getString("status"),
            formatTime(rs.getTimestamp("bound_at"))
        ), primaryAccountNo, primaryAccountType, roleFilter), traceId());
    }

    @GetMapping("/primary/profile")
    public ApiResponse<TwentyMallPrimaryProfileResponse> primaryProfile(
        @RequestParam String accountNo,
        @RequestParam String accountType
    ) {
        ensurePrimaryAccount(accountNo, accountType, accountNo, null, "PHONE_CODE");
        String sql = """
            SELECT id, account_no, account_type, display_name, phone, avatar_url, login_mode, status
            FROM primary_account
            WHERE account_no = ? AND account_type = ? AND deleted = 0
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "一级账号不存在", traceId());
            }
            Long bindingCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM platform_account_binding
                WHERE primary_account_id = ?
                  AND bind_status = 'BOUND'
                  AND deleted = 0
                """,
                Long.class,
                rs.getLong("id")
            );
            return ApiResponse.success(new TwentyMallPrimaryProfileResponse(
                rs.getString("account_no"),
                rs.getString("account_type"),
                rs.getString("display_name"),
                rs.getString("phone"),
                normalizedPrimaryAvatar(rs.getString("avatar_url")),
                rs.getString("login_mode"),
                rs.getString("status"),
                bindingCount == null ? 0 : bindingCount
            ), traceId());
        }, accountNo, accountType);
    }

    @PostMapping("/primary/profile")
    public ApiResponse<TwentyMallPrimaryProfileResponse> savePrimaryProfile(
        @Valid @RequestBody TwentyMallPrimaryProfileSaveRequest request
    ) {
        ensurePrimaryAccount(
            request.accountNo(),
            request.accountType(),
            cleanDisplayName(request.displayName(), request.accountNo()),
            request.avatar(),
            "PHONE_CODE"
        );
        jdbcTemplate.update(
            """
            UPDATE primary_account
            SET display_name = ?, avatar_url = ?, phone = account_no, updated_at = NOW()
            WHERE account_no = ? AND account_type = ? AND deleted = 0
            """,
            cleanDisplayName(request.displayName(), request.accountNo()),
            persistablePrimaryAvatar(request.avatar()),
            request.accountNo(),
            request.accountType()
        );
        return primaryProfile(request.accountNo(), request.accountType());
    }

    private List<TwentyMallAdminBindingResponse> loadAdminBindingRows(String accountType) {
        String sql = """
            SELECT pa.account_no AS primary_account_no,
                   pa.display_name AS primary_display_name,
                   pa.avatar_url AS primary_avatar,
                   b.platform_name,
                   b.secondary_account_no,
                   tm.display_name AS secondary_display_name,
                   b.bind_status,
                   tm.status,
                   b.bound_at
            FROM platform_account_binding b
            JOIN primary_account pa ON pa.id = b.primary_account_id
            LEFT JOIN twenty_mall_account tm ON tm.account_no = b.secondary_account_no
              AND tm.account_role = b.secondary_account_role
              AND tm.deleted = 0
            WHERE pa.account_type = ?
              AND pa.deleted = 0
              AND b.deleted = 0
              AND b.bind_status = 'BOUND'
            ORDER BY pa.account_no, b.platform_code, b.secondary_account_no
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TwentyMallAdminBindingResponse(
            rs.getString("primary_account_no"),
            rs.getString("primary_display_name"),
            normalizedPrimaryAvatar(rs.getString("primary_avatar")),
            rs.getString("platform_name"),
            rs.getString("secondary_account_no"),
            rs.getString("secondary_display_name"),
            bindStatusText(rs.getString("bind_status")),
            rs.getString("status"),
            formatTime(rs.getTimestamp("bound_at"))
        ), accountType);
    }

    private TwentyMallAdminSyncLogResponse buildSyncLog(String task, String countSql, String timeSql) {
        Long count = jdbcTemplate.queryForObject(countSql, Long.class);
        Timestamp latestTime = jdbcTemplate.queryForObject(timeSql, Timestamp.class);
        long normalizedCount = count == null ? 0 : count;
        return new TwentyMallAdminSyncLogResponse(
            task,
            normalizedCount > 0 && latestTime != null ? "正常" : "暂无数据",
            normalizedCount,
            formatTime(latestTime)
        );
    }

    private Long countLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private void ensureReviewDisputeTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS twenty_mall_review_dispute (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                review_id BIGINT NOT NULL,
                merchant_account_id BIGINT NOT NULL,
                reason TEXT NOT NULL,
                status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                admin_note TEXT NULL,
                reviewed_at DATETIME NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                deleted TINYINT(1) NOT NULL DEFAULT 0,
                UNIQUE KEY uk_twenty_mall_review_dispute_review (review_id),
                KEY idx_twenty_mall_review_dispute_merchant (merchant_account_id),
                KEY idx_twenty_mall_review_dispute_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='20商城评价异议'
            """);
    }

    private List<TwentyMallAdminTrendResponse> loadAdminTrendRows() {
        List<TwentyMallAdminTrendResponse> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Long orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM twenty_mall_order WHERE deleted = 0 AND DATE(ordered_at) = ?",
                Long.class,
                day
            );
            Long afterSaleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM twenty_mall_after_sale WHERE deleted = 0 AND DATE(created_at) = ?",
                Long.class,
                day
            );
            Long reviewCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM twenty_mall_review WHERE deleted = 0 AND DATE(COALESCE(reviewed_at, created_at)) = ?",
                Long.class,
                day
            );
            rows.add(new TwentyMallAdminTrendResponse(
                day.format(DateTimeFormatter.ofPattern("M.d")),
                orderCount == null ? 0 : orderCount,
                afterSaleCount == null ? 0 : afterSaleCount,
                reviewCount == null ? 0 : reviewCount
            ));
        }
        return rows;
    }

    private List<TwentyMallAdminActivityResponse> loadAdminActivityRows() {
        String sql = """
            SELECT module, title, content, activity_time
            FROM (
              SELECT '评价分析' AS module,
                     CONCAT('订单 ', o.order_no, ' 收到新评价') AS title,
                     CONCAT(ma.display_name, ' · ', p.product_name) AS content,
                     COALESCE(r.reviewed_at, r.created_at) AS activity_time
              FROM twenty_mall_review r
              JOIN twenty_mall_order o ON o.id = r.order_id
              JOIN twenty_mall_product p ON p.id = r.product_id
              JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
              WHERE r.deleted = 0
              UNION ALL
              SELECT '规则配置' AS module,
                     CONCAT('售后规则已更新：', rule_name) AS title,
                     COALESCE(content, rule_type) AS content,
                     updated_at AS activity_time
              FROM after_sale_rule
              WHERE deleted = 0
              UNION ALL
              SELECT '知识库' AS module,
                     CONCAT('知识文章已更新：', title) AS title,
                     category AS content,
                     updated_at AS activity_time
              FROM knowledge_article
              WHERE deleted = 0
              UNION ALL
              SELECT '知识库' AS module,
                     CONCAT('常见问题已更新：', question) AS title,
                     category AS content,
                     updated_at AS activity_time
              FROM faq_item
              WHERE deleted = 0
            ) activity
            ORDER BY activity_time DESC
            LIMIT 6
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TwentyMallAdminActivityResponse(
            rowNum + 1L,
            rs.getString("module"),
            rs.getString("title"),
            cleanProductName(rs.getString("content")),
            formatTime(rs.getTimestamp("activity_time"))
        ));
    }

    private ApiResponse<TwentyMallAdminRuleResponse> adminRuleById(Long ruleId) {
        String sql = """
            SELECT id, rule_name, rule_type, conditions_json, action_json, content, enabled, updated_at
            FROM after_sale_rule
            WHERE id = ? AND deleted = 0
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "规则不存在或已删除", traceId());
            }
            return ApiResponse.success(new TwentyMallAdminRuleResponse(
                rs.getLong("id"),
                rs.getString("rule_name"),
                rs.getString("rule_type"),
                ruleTypeText(rs.getString("rule_type")),
                rs.getString("conditions_json"),
                ruleConditionText(rs.getString("conditions_json")),
                rs.getString("action_json"),
                ruleActionText(rs.getString("action_json")),
                rs.getString("content"),
                rs.getBoolean("enabled"),
                formatTime(rs.getTimestamp("updated_at"))
            ), traceId());
        }, ruleId);
    }

    @PostMapping("/login")
    public ApiResponse<TwentyMallLoginResponse> login(@Valid @RequestBody TwentyMallLoginRequest request) {
        String sql = """
            SELECT account_no, account_role, display_name, phone, bind_status
            FROM twenty_mall_account
            WHERE account_no = ? AND password_plain = ? AND account_role = ? AND status = 'ACTIVE'
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("401", "20商城账号或密码错误", traceId());
            }
            TwentyMallLoginResponse response = new TwentyMallLoginResponse(
                rs.getString("account_no"),
                rs.getString("account_role"),
                rs.getString("display_name"),
                rs.getString("phone"),
                rs.getString("bind_status")
            );
            return ApiResponse.success(response, traceId());
        }, request.accountNo(), request.password(), request.role());
    }

    @PostMapping("/bind")
    public ApiResponse<Map<String, String>> bind(@Valid @RequestBody TwentyMallBindRequest request) {
        ApiResponse<TwentyMallLoginResponse> loginResult = login(new TwentyMallLoginRequest(
            request.accountNo(),
            request.password(),
            request.role()
        ));
        if (!"200".equals(loginResult.code())) {
            return ApiResponse.fail(loginResult.code(), loginResult.message(), traceId());
        }
        if (request.primaryAccountNo() != null && !request.primaryAccountNo().isBlank() && hasConflictingPrimaryBinding(request)) {
            return ApiResponse.fail("409", "该账号已被其他一级账号绑定", traceId());
        }
        jdbcTemplate.update(
            "UPDATE twenty_mall_account SET bind_status = 'BOUND', updated_at = NOW() WHERE account_no = ? AND account_role = ?",
            request.accountNo(),
            request.role()
        );
        if (request.primaryAccountNo() != null && !request.primaryAccountNo().isBlank()) {
            persistPrimaryBinding(request);
        }
        return ApiResponse.success(Map.of("bindStatus", "BOUND"), traceId());
    }

    @PostMapping("/unbind")
    public ApiResponse<Map<String, String>> unbind(@Valid @RequestBody TwentyMallUnbindRequest request) {
        Long primaryId = jdbcTemplate.query(
            "SELECT id FROM primary_account WHERE account_no = ? AND account_type = ? AND deleted = 0 LIMIT 1",
            rs -> rs.next() ? rs.getLong("id") : null,
            request.primaryAccountNo(),
            request.primaryAccountType()
        );
        if (primaryId == null) {
            return ApiResponse.fail("404", "一级账号不存在", traceId());
        }
        jdbcTemplate.update(
            """
            UPDATE platform_account_binding
            SET bind_status = 'UNBOUND', unbound_at = NOW(), deleted = 1, updated_at = NOW()
            WHERE primary_account_id = ?
              AND platform_code = 'TWENTY_MALL'
              AND secondary_account_no = ?
              AND secondary_account_role = ?
              AND deleted = 0
            """,
            primaryId,
            request.accountNo(),
            request.role()
        );
        Long activeCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM platform_account_binding
            WHERE platform_code = 'TWENTY_MALL'
              AND secondary_account_no = ?
              AND secondary_account_role = ?
              AND bind_status = 'BOUND'
              AND deleted = 0
            """,
            Long.class,
            request.accountNo(),
            request.role()
        );
        if (activeCount == null || activeCount == 0) {
            jdbcTemplate.update(
                "UPDATE twenty_mall_account SET bind_status = 'UNBOUND', updated_at = NOW() WHERE account_no = ? AND account_role = ?",
                request.accountNo(),
                request.role()
            );
        }
        return ApiResponse.success(Map.of("bindStatus", "UNBOUND"), traceId());
    }

    private boolean hasConflictingPrimaryBinding(TwentyMallBindRequest request) {
        Long count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM platform_account_binding b
            JOIN primary_account pa ON pa.id = b.primary_account_id
            WHERE b.platform_code = 'TWENTY_MALL'
              AND b.secondary_account_no = ?
              AND b.secondary_account_role = ?
              AND b.bind_status = 'BOUND'
              AND b.deleted = 0
              AND NOT (pa.account_no = ? AND pa.account_type = ?)
            """,
            Long.class,
            request.accountNo(),
            request.role(),
            request.primaryAccountNo(),
            request.primaryAccountType() == null || request.primaryAccountType().isBlank() ? request.role() : request.primaryAccountType()
        );
        return count != null && count > 0;
    }

    private void persistPrimaryBinding(TwentyMallBindRequest request) {
        String primaryType = request.primaryAccountType() == null || request.primaryAccountType().isBlank()
            ? request.role()
            : request.primaryAccountType();
        ensurePrimaryAccount(request.primaryAccountNo(), primaryType, request.primaryDisplayName(), null, "DEMO");
        Long primaryId = jdbcTemplate.queryForObject(
            "SELECT id FROM primary_account WHERE account_no = ? AND account_type = ? AND deleted = 0 LIMIT 1",
            Long.class,
            request.primaryAccountNo(),
            primaryType
        );
        jdbcTemplate.update(
            """
            INSERT INTO platform_account_binding (
              primary_account_id, platform_code, platform_name, secondary_account_no, secondary_account_role, bind_status, bound_at, deleted
            ) VALUES (?, 'TWENTY_MALL', '20商城', ?, ?, 'BOUND', NOW(), 0)
            ON DUPLICATE KEY UPDATE
              primary_account_id = VALUES(primary_account_id),
              bind_status = 'BOUND',
              unbound_at = NULL,
              deleted = 0,
              updated_at = NOW()
            """,
            primaryId,
            request.accountNo(),
            request.role()
        );
    }

    private void ensurePrimaryAccount(String accountNo, String accountType, String displayName, String avatar, String loginMode) {
        String cleanName = cleanDisplayName(displayName, accountNo);
        jdbcTemplate.update(
            """
            INSERT INTO primary_account (account_no, account_type, display_name, phone, avatar_url, login_mode, status, deleted)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 0)
            ON DUPLICATE KEY UPDATE
              display_name = CASE
                WHEN VALUES(display_name) IS NULL OR VALUES(display_name) = '' OR VALUES(display_name) = account_no THEN display_name
                ELSE VALUES(display_name)
              END,
              phone = account_no,
              avatar_url = CASE
                WHEN VALUES(avatar_url) IS NULL OR VALUES(avatar_url) = '' THEN avatar_url
                ELSE VALUES(avatar_url)
              END,
              status = 'ACTIVE',
              deleted = 0,
              updated_at = NOW()
            """,
            accountNo,
            accountType,
            cleanName,
            accountNo,
            persistablePrimaryAvatar(avatar),
            loginMode
        );
    }

    private String persistablePrimaryAvatar(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return null;
        }
        String trimmed = avatar.trim();
        if (trimmed.startsWith("http://tmp/") || trimmed.startsWith("wxfile://")) {
            return null;
        }
        return trimmed;
    }

    private String normalizedPrimaryAvatar(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return null;
        }
        String trimmed = avatar.trim();
        if (trimmed.startsWith("http://tmp/") || trimmed.startsWith("wxfile://")) {
            return null;
        }
        return trimmed;
    }

    private String cleanDisplayName(String displayName, String accountNo) {
        if (displayName == null || displayName.isBlank()) {
            return accountNo;
        }
        String trimmed = displayName.trim();
        if (trimmed.startsWith("消费者一级账号 ") || trimmed.startsWith("商家一级账号 ")) {
            return accountNo;
        }
        return trimmed;
    }

    @GetMapping("/profile")
    public ApiResponse<TwentyMallProfileResponse> profile(@RequestParam String accountNo, @RequestParam String role) {
        String sql = """
            SELECT account_no, account_role, display_name, phone, bind_status
            FROM twenty_mall_account
            WHERE account_no = ? AND account_role = ? AND status = 'ACTIVE'
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "20商城账号不存在", traceId());
            }
            return ApiResponse.success(new TwentyMallProfileResponse(
                rs.getString("account_no"),
                rs.getString("account_role"),
                rs.getString("display_name"),
                rs.getString("phone"),
                rs.getString("bind_status"),
                "/assets/avatars/twenty-user.png",
                profileAddress(rs.getString("account_no"))
            ), traceId());
        }, accountNo, role);
    }

    @GetMapping("/consumer/orders")
    public ApiResponse<List<TwentyMallOrderResponse>> consumerOrders(@RequestParam String accountNo) {
        String sql = """
            SELECT o.order_no, o.order_status, o.pay_status, o.logistics_status, o.after_sale_status,
                   o.total_amount, o.ordered_at, o.delivered_at, o.policy_tags,
                   i.product_name, i.sku_name, i.product_image_url,
                   ma.account_no AS merchant_account_no,
                   mpa.account_no AS merchant_primary_account_no,
                   ma.display_name AS merchant_name
            FROM twenty_mall_order o
            JOIN twenty_mall_account a ON a.id = o.consumer_account_id
            JOIN twenty_mall_order_item i ON i.order_id = o.id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            LEFT JOIN platform_account_binding mb ON mb.secondary_account_no = ma.account_no
                AND mb.secondary_account_role = 'MERCHANT'
                AND mb.bind_status = 'BOUND'
                AND mb.deleted = 0
            LEFT JOIN primary_account mpa ON mpa.id = mb.primary_account_id
                AND mpa.account_type = 'MERCHANT'
                AND mpa.deleted = 0
            WHERE a.account_no = ? AND o.deleted = 0 AND i.deleted = 0
            ORDER BY o.ordered_at DESC
            """;
        return ApiResponse.success(jdbcTemplate.query(sql, (rs, rowNum) -> new TwentyMallOrderResponse(
            rs.getString("order_no"),
            rs.getString("product_name"),
            rs.getString("sku_name"),
            rs.getString("product_image_url"),
            rs.getBigDecimal("total_amount"),
            orderStatusText(rs.getString("order_status")),
            payStatusText(rs.getString("pay_status")),
            logisticsStatusText(rs.getString("logistics_status")),
            afterSaleStatusText(effectiveAfterSaleStatus(rs.getString("order_no"), rs.getString("after_sale_status"))),
            rs.getString("merchant_name"),
            rs.getString("merchant_account_no"),
            rs.getString("merchant_primary_account_no"),
            formatTime(rs.getTimestamp("ordered_at")),
            shouldShowDeliveredAt(rs.getString("order_status"), rs.getString("logistics_status")) ? formatTime(rs.getTimestamp("delivered_at")) : "",
            parsePolicyTags(rs.getString("policy_tags")),
            hasPublishedReview(rs.getString("order_no"))
        ), accountNo), traceId());
    }

    @GetMapping("/merchant/orders")
    public ApiResponse<List<TwentyMallMerchantOrderResponse>> merchantOrders(@RequestParam String accountNo) {
        String sql = """
            SELECT o.id, o.order_no, ca.display_name AS buyer_name, o.order_status, o.pay_status, o.logistics_status,
                   o.after_sale_status, o.total_amount, o.ordered_at, i.product_name, ma.display_name AS merchant_name
            FROM twenty_mall_order o
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            JOIN twenty_mall_account ca ON ca.id = o.consumer_account_id
            JOIN twenty_mall_order_item i ON i.order_id = o.id
            WHERE ma.account_no = ? AND ma.account_role = 'MERCHANT' AND o.deleted = 0 AND i.deleted = 0
            ORDER BY o.ordered_at DESC
            """;
        return ApiResponse.success(jdbcTemplate.query(sql, (rs, rowNum) -> new TwentyMallMerchantOrderResponse(
            rs.getLong("id"),
            rs.getString("order_no"),
            rs.getString("buyer_name"),
            orderStatusText(rs.getString("order_status")),
            payStatusText(rs.getString("pay_status")),
            logisticsStatusText(rs.getString("logistics_status")),
            afterSaleStatusText(effectiveAfterSaleStatus(rs.getString("order_no"), rs.getString("after_sale_status"))),
            rs.getBigDecimal("total_amount"),
            formatTime(rs.getTimestamp("ordered_at")),
            rs.getString("product_name"),
            rs.getString("merchant_name")
        ), accountNo), traceId());
    }

    @GetMapping("/merchant/reviews")
    public ApiResponse<List<TwentyMallMerchantReviewResponse>> merchantReviews(@RequestParam String accountNo) {
        ensureReviewDisputeTable();
        String sql = """
            SELECT r.id, r.product_score, r.service_score, r.content, r.reviewed_at, r.deleted AS review_deleted,
                   o.order_no, p.product_name, ma.display_name AS merchant_name,
                   d.id AS dispute_id, d.status AS dispute_status, d.reason AS dispute_reason,
                   d.admin_note AS dispute_admin_note
            FROM twenty_mall_review r
            JOIN twenty_mall_order o ON o.id = r.order_id
            JOIN twenty_mall_product p ON p.id = r.product_id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            LEFT JOIN twenty_mall_review_dispute d ON d.review_id = r.id AND d.deleted = 0
            WHERE ma.account_no = ? AND ma.account_role = 'MERCHANT' AND o.deleted = 0
            ORDER BY r.deleted ASC, r.reviewed_at DESC, r.id DESC
            """;
        return ApiResponse.success(jdbcTemplate.query(sql, (rs, rowNum) -> {
            String content = rs.getString("content");
            int productScore = rs.getInt("product_score");
            int serviceScore = rs.getInt("service_score");
            String sentiment = reviewSentiment(productScore, serviceScore, content);
            String riskLevel = reviewRiskLevel(productScore, serviceScore, content);
            return new TwentyMallMerchantReviewResponse(
                rs.getLong("id"),
                "20商城",
                rs.getString("order_no"),
                rs.getString("product_name"),
                rs.getString("merchant_name"),
                productScore,
                serviceScore,
                content,
                sentiment,
                riskLevel,
                reviewKeywords(content, productScore),
                reviewSummary(sentiment, riskLevel, content),
                reviewSuggestion(riskLevel),
                rs.getLong("dispute_id") == 0 ? null : rs.getLong("dispute_id"),
                disputeStatusText(rs.getString("dispute_status")),
                rs.getString("dispute_reason"),
                rs.getString("dispute_admin_note"),
                rs.getBoolean("review_deleted")
            );
        }, accountNo), traceId());
    }

    @GetMapping("/merchant/notifications")
    public ApiResponse<List<TwentyMallMerchantNotificationResponse>> merchantNotifications(@RequestParam String accountNo) {
        ensureReviewDisputeTable();
        String sql = """
            SELECT *
            FROM (
              SELECT
                CONCAT('after-sale-', a.id) AS id,
                'AFTER_SALE' AS notice_type,
                CONCAT('顾客发起售后：', o.order_no) AS title,
                CONCAT('商品：', i.product_name, '；原因：', a.reason_type, '；状态：', a.status) AS summary,
                COALESCE(NULLIF(a.description, ''), a.reason_type) AS detail,
                a.id AS related_id,
                o.order_no AS related_no,
                '/after-sales' AS target_path,
                a.status AS status_code,
                NULL AS risk_level,
                a.created_at AS occurred_at
              FROM twenty_mall_after_sale a
              JOIN twenty_mall_order o ON o.id = a.order_id
              JOIN twenty_mall_order_item i ON i.id = a.order_item_id
              JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
              WHERE ma.account_no = ?
                AND ma.account_role = 'MERCHANT'
                AND a.deleted = 0
                AND o.deleted = 0
                AND i.deleted = 0
              UNION ALL
              SELECT
                CONCAT('review-', r.id) AS id,
                'REVIEW' AS notice_type,
                CONCAT('顾客给出评价：', o.order_no) AS title,
                CONCAT('商品：', p.product_name, '；产品质量 ', r.product_score, ' 星，商家服务 ', r.service_score, ' 星') AS summary,
                r.content AS detail,
                r.id AS related_id,
                o.order_no AS related_no,
                '/reviews' AS target_path,
                CASE
                  WHEN r.product_score <= 2 OR r.service_score <= 2 THEN 'HIGH'
                  WHEN r.product_score <= 3 OR r.service_score <= 3 THEN 'MEDIUM'
                  ELSE 'LOW'
                END AS status_code,
                CASE
                  WHEN r.product_score <= 2 OR r.service_score <= 2 THEN 'HIGH'
                  WHEN r.product_score <= 3 OR r.service_score <= 3 THEN 'MEDIUM'
                  ELSE 'LOW'
                END AS risk_level,
                COALESCE(r.reviewed_at, r.created_at) AS occurred_at
              FROM twenty_mall_review r
              JOIN twenty_mall_order o ON o.id = r.order_id
              JOIN twenty_mall_product p ON p.id = r.product_id
              JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
              WHERE ma.account_no = ?
                AND ma.account_role = 'MERCHANT'
                AND r.deleted = 0
                AND o.deleted = 0
              UNION ALL
              SELECT
                CONCAT('review-dispute-', d.id) AS id,
                'REVIEW_DISPUTE' AS notice_type,
                CONCAT('评价异议审核结果：', o.order_no) AS title,
                CONCAT('管理员已', CASE d.status WHEN 'APPROVED' THEN '通过异议并删除评价' WHEN 'REJECTED' THEN '拒绝异议并保留评价' ELSE '收到异议申请' END) AS summary,
                CONCAT('异议原因：', d.reason, IF(d.admin_note IS NULL OR d.admin_note = '', '', CONCAT('；审核说明：', d.admin_note))) AS detail,
                d.id AS related_id,
                o.order_no AS related_no,
                '/reviews' AS target_path,
                d.status AS status_code,
                CASE
                  WHEN r.product_score <= 2 OR r.service_score <= 2 THEN 'HIGH'
                  WHEN r.product_score <= 3 OR r.service_score <= 3 THEN 'MEDIUM'
                  ELSE 'LOW'
                END AS risk_level,
                COALESCE(d.reviewed_at, d.updated_at, d.created_at) AS occurred_at
              FROM twenty_mall_review_dispute d
              JOIN twenty_mall_review r ON r.id = d.review_id
              JOIN twenty_mall_order o ON o.id = r.order_id
              JOIN twenty_mall_account ma ON ma.id = d.merchant_account_id
              WHERE ma.account_no = ?
                AND ma.account_role = 'MERCHANT'
                AND d.deleted = 0
            ) notices
            ORDER BY occurred_at DESC, id DESC
            LIMIT 30
            """;
        return ApiResponse.success(jdbcTemplate.query(sql, (rs, rowNum) -> {
            String type = rs.getString("notice_type");
            String statusCode = rs.getString("status_code");
            return new TwentyMallMerchantNotificationResponse(
                rs.getString("id"),
                type,
                notificationTypeText(type),
                rs.getString("title"),
                notificationSummaryText(type, rs.getString("summary")),
                notificationDetailText(type, rs.getString("detail")),
                rs.getLong("related_id"),
                rs.getString("related_no"),
                rs.getString("target_path"),
                statusCode,
                rs.getString("risk_level"),
                notificationStatusText(type, statusCode),
                notificationTone(type, statusCode),
                formatTime(rs.getTimestamp("occurred_at"))
            );
        }, accountNo, accountNo, accountNo), traceId());
    }

    @PostMapping("/merchant/reviews/{reviewId}/dispute")
    public ApiResponse<Map<String, Object>> submitMerchantReviewDispute(
        @PathVariable Long reviewId,
        @RequestBody TwentyMallReviewDisputeSubmitRequest request
    ) {
        ensureReviewDisputeTable();
        if (request.reason() == null || request.reason().trim().isBlank()) {
            return ApiResponse.fail("400", "请填写异议原因", traceId());
        }
        List<Long> merchantIds = jdbcTemplate.query(
            """
            SELECT o.merchant_account_id
            FROM twenty_mall_review r
            JOIN twenty_mall_order o ON o.id = r.order_id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            WHERE r.id = ? AND r.deleted = 0 AND ma.account_no = ? AND ma.account_role = 'MERCHANT'
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getLong("merchant_account_id"),
            reviewId,
            request.accountNo()
        );
        if (merchantIds.isEmpty()) {
            return ApiResponse.fail("404", "评价不存在或不属于当前商家", traceId());
        }
        jdbcTemplate.update(
            """
            INSERT INTO twenty_mall_review_dispute (review_id, merchant_account_id, reason, status)
            VALUES (?, ?, ?, 'PENDING')
            ON DUPLICATE KEY UPDATE
              reason = VALUES(reason),
              status = 'PENDING',
              admin_note = NULL,
              reviewed_at = NULL,
              deleted = 0,
              updated_at = NOW()
            """,
            reviewId,
            merchantIds.get(0),
            request.reason().trim()
        );
        Long disputeId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (disputeId == null || disputeId == 0) {
            disputeId = jdbcTemplate.queryForObject(
                "SELECT id FROM twenty_mall_review_dispute WHERE review_id = ? AND deleted = 0 LIMIT 1",
                Long.class,
                reviewId
            );
        }
        return ApiResponse.success(Map.of("disputeId", disputeId == null ? 0 : disputeId, "status", "PENDING"), traceId());
    }

    @PostMapping("/merchant/reviews/{reviewId}/analyze")
    public ApiResponse<Map<String, Object>> analyzeMerchantReview(
        @PathVariable Long reviewId,
        @RequestParam String accountNo
    ) {
        String sql = """
            SELECT r.content, r.product_score, r.service_score, ma.account_no
            FROM twenty_mall_review r
            JOIN twenty_mall_order o ON o.id = r.order_id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            WHERE r.id = ? AND r.deleted = 0 AND ma.account_no = ? AND ma.account_role = 'MERCHANT'
            LIMIT 1
            """;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, reviewId, accountNo);
        if (results.isEmpty()) {
            return ApiResponse.fail("404", "评价不存在或不属于当前商家", traceId());
        }
        Map<String, Object> row = results.get(0);
        String content = row.get("content") != null ? row.get("content").toString() : "";
        int productScore = row.get("product_score") != null ? ((Number) row.get("product_score")).intValue() : 5;
        int serviceScore = row.get("service_score") != null ? ((Number) row.get("service_score")).intValue() : 5;

        ReviewAnalysisResponse aiResponse = aiService.analyzeReview(new AiTextRequest(content, null, "REVIEW", reviewId));

        String sentiment = aiResponse.sentiment() != null ? aiResponse.sentiment() : reviewSentiment(productScore, serviceScore, content);
        String riskLevel = aiResponse.riskLevel() != null ? aiResponse.riskLevel() : reviewRiskLevel(productScore, serviceScore, content);
        String keywords = aiResponse.keywords() != null && !aiResponse.keywords().isEmpty()
            ? String.join("、", aiResponse.keywords())
            : reviewKeywords(content, productScore);
        String summary = aiResponse.summary() != null && !aiResponse.summary().isBlank()
            ? aiResponse.summary()
            : reviewSummary(sentiment, riskLevel, content);
        String suggestion = aiResponse.suggestion() != null && !aiResponse.suggestion().isBlank()
            ? aiResponse.suggestion()
            : reviewSuggestion(riskLevel);

        return ApiResponse.success(Map.of(
            "sentiment", sentiment,
            "riskLevel", riskLevel,
            "keywords", keywords,
            "summary", summary,
            "suggestion", suggestion
        ), traceId());
    }

    @GetMapping("/merchant/after-sales")
    public ApiResponse<List<TwentyMallAfterSaleResponse>> merchantAfterSales(@RequestParam String accountNo) {
        String sql = """
            SELECT a.id, a.after_sale_no, o.order_no, a.after_sale_type, a.reason_type, a.description,
                   a.requested_amount, a.status, a.created_at,
                   i.product_name, ma.display_name AS merchant_name
            FROM twenty_mall_after_sale a
            JOIN twenty_mall_order o ON o.id = a.order_id
            JOIN twenty_mall_order_item i ON i.id = a.order_item_id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            WHERE ma.account_no = ? AND ma.account_role = 'MERCHANT'
              AND a.deleted = 0 AND o.deleted = 0 AND i.deleted = 0
            ORDER BY a.created_at DESC, a.id DESC
            """;
        return ApiResponse.success(jdbcTemplate.query(sql, (rs, rowNum) -> {
            AfterSaleDescription description = parseAfterSaleDescription(rs.getString("description"));
            return new TwentyMallAfterSaleResponse(
                rs.getLong("id"),
                rs.getString("after_sale_no"),
                rs.getString("order_no"),
                rs.getString("after_sale_type"),
                rs.getString("reason_type"),
                rs.getBigDecimal("requested_amount"),
                rs.getString("status"),
                afterSalePriority(rs.getString("reason_type")),
                afterSaleReviewOpinion(rs.getString("status"), description.reason(), description.reviewReason()),
                "PENDING",
                formatTime(rs.getTimestamp("created_at")),
                rs.getString("product_name"),
                "TWENTY_MALL",
                "20商城",
                rs.getString("merchant_name"),
                description.reason(),
                description.evidenceImages()
            );
        }, accountNo), traceId());
    }

    @PostMapping("/consumer/after-sales")
    public ApiResponse<TwentyMallAfterSaleResponse> applyAfterSale(@Valid @RequestBody TwentyMallAfterSaleApplyRequest request) {
        String orderSql = """
            SELECT o.id AS order_id, o.order_no, o.total_amount, i.id AS item_id, i.product_name, ma.display_name AS merchant_name
            FROM twenty_mall_order o
            JOIN twenty_mall_order_item i ON i.order_id = o.id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            WHERE o.order_no = ? AND o.deleted = 0 AND i.deleted = 0
            LIMIT 1
            """;
        return jdbcTemplate.query(orderSql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "订单不存在，无法发起售后", traceId());
            }
            long orderId = rs.getLong("order_id");
            long itemId = rs.getLong("item_id");
            String orderNo = rs.getString("order_no");
            BigDecimal requestedAmount = rs.getBigDecimal("total_amount");
            String productName = rs.getString("product_name");
            String merchantName = rs.getString("merchant_name");
            String afterSaleNo = existingAfterSaleNo(orderId, itemId);
            if (afterSaleNo == null) {
                afterSaleNo = "TMAS" + System.currentTimeMillis();
            }
            List<String> evidenceUrls = saveEvidenceImages(afterSaleNo, request.evidenceImages());
            String descriptionPayload = buildAfterSaleDescription(request.description(), evidenceUrls);
            if (afterSaleId(afterSaleNo) == 0L) {
                jdbcTemplate.update(
                    """
                    INSERT INTO twenty_mall_after_sale (
                      after_sale_no, order_id, order_item_id, after_sale_type, reason_type,
                      description, requested_amount, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING_REVIEW')
                    """,
                    afterSaleNo,
                    orderId,
                    itemId,
                    request.afterSaleType(),
                    request.reasonType(),
                    descriptionPayload,
                    requestedAmount
                );
            } else {
                jdbcTemplate.update(
                    """
                    UPDATE twenty_mall_after_sale
                    SET after_sale_type = ?, reason_type = ?, description = ?, requested_amount = ?,
                        status = 'PENDING_REVIEW', updated_at = NOW(), deleted = 0
                    WHERE after_sale_no = ?
                    """,
                    request.afterSaleType(),
                    request.reasonType(),
                    descriptionPayload,
                    requestedAmount,
                    afterSaleNo
                );
            }
            jdbcTemplate.update("UPDATE twenty_mall_order SET after_sale_status = 'AFTER_SALE', updated_at = NOW() WHERE id = ?", orderId);
            jdbcTemplate.update("UPDATE twenty_mall_order_item SET after_sale_status = 'APPLIED', updated_at = NOW() WHERE id = ?", itemId);
            return ApiResponse.success(new TwentyMallAfterSaleResponse(
                afterSaleId(afterSaleNo),
                afterSaleNo,
                orderNo,
                request.afterSaleType(),
                request.reasonType(),
                requestedAmount,
                "PENDING_REVIEW",
                afterSalePriority(request.reasonType()),
                afterSaleReviewOpinion("PROCESSING", request.description()),
                "PENDING",
                formatTime(new Timestamp(System.currentTimeMillis())),
                productName,
                "TWENTY_MALL",
                "20商城",
                merchantName,
                request.description(),
                evidenceUrls
            ), traceId());
        }, request.orderNo());
    }

    @PostMapping("/consumer/after-sales/cancel")
    public ApiResponse<Map<String, String>> cancelAfterSale(@Valid @RequestBody TwentyMallAfterSaleCancelRequest request) {
        String orderSql = """
            SELECT o.id AS order_id, i.id AS item_id
            FROM twenty_mall_order o
            JOIN twenty_mall_order_item i ON i.order_id = o.id
            WHERE o.order_no = ? AND o.deleted = 0 AND i.deleted = 0
            LIMIT 1
            """;
        return jdbcTemplate.query(orderSql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "订单不存在，无法取消售后", traceId());
            }
            long orderId = rs.getLong("order_id");
            long itemId = rs.getLong("item_id");
            jdbcTemplate.update(
                "UPDATE twenty_mall_after_sale SET deleted = 1, status = 'CLOSED', updated_at = NOW() WHERE order_id = ? AND order_item_id = ? AND deleted = 0",
                orderId,
                itemId
            );
            jdbcTemplate.update("UPDATE twenty_mall_order SET after_sale_status = 'NONE', updated_at = NOW() WHERE id = ?", orderId);
            jdbcTemplate.update("UPDATE twenty_mall_order_item SET after_sale_status = 'NONE', updated_at = NOW() WHERE id = ?", itemId);
            return ApiResponse.success(Map.of("afterSaleStatus", "NONE"), traceId());
        }, request.orderNo());
    }

    @GetMapping("/evidence/{fileName}")
    public ResponseEntity<byte[]> evidenceImage(@PathVariable String fileName) throws IOException {
        Path file = EVIDENCE_DIR.resolve(fileName).normalize();
        if (!file.startsWith(EVIDENCE_DIR) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = fileName.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(Files.readAllBytes(file));
    }

    @GetMapping("/consumer/orders/detail")
    public ApiResponse<TwentyMallOrderResponse> orderDetail(@RequestParam String orderNo) {
        String sql = """
            SELECT o.order_no, o.order_status, o.pay_status, o.logistics_status, o.after_sale_status,
                   o.total_amount, o.ordered_at, o.delivered_at, o.policy_tags,
                   i.product_name, i.sku_name, i.product_image_url,
                   ma.account_no AS merchant_account_no,
                   mpa.account_no AS merchant_primary_account_no,
                   ma.display_name AS merchant_name
            FROM twenty_mall_order o
            JOIN twenty_mall_order_item i ON i.order_id = o.id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            LEFT JOIN platform_account_binding mb ON mb.secondary_account_no = ma.account_no
                AND mb.secondary_account_role = 'MERCHANT'
                AND mb.bind_status = 'BOUND'
                AND mb.deleted = 0
            LEFT JOIN primary_account mpa ON mpa.id = mb.primary_account_id
                AND mpa.account_type = 'MERCHANT'
                AND mpa.deleted = 0
            WHERE o.order_no = ? AND o.deleted = 0 AND i.deleted = 0
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "20商城订单不存在", traceId());
            }
            return ApiResponse.success(new TwentyMallOrderResponse(
                rs.getString("order_no"),
                rs.getString("product_name"),
                rs.getString("sku_name"),
                rs.getString("product_image_url"),
                rs.getBigDecimal("total_amount"),
                orderStatusText(rs.getString("order_status")),
                payStatusText(rs.getString("pay_status")),
                logisticsStatusText(rs.getString("logistics_status")),
                afterSaleStatusText(effectiveAfterSaleStatus(rs.getString("order_no"), rs.getString("after_sale_status"))),
                rs.getString("merchant_name"),
                rs.getString("merchant_account_no"),
                rs.getString("merchant_primary_account_no"),
                formatTime(rs.getTimestamp("ordered_at")),
                shouldShowDeliveredAt(rs.getString("order_status"), rs.getString("logistics_status")) ? formatTime(rs.getTimestamp("delivered_at")) : "",
                parsePolicyTags(rs.getString("policy_tags")),
                hasPublishedReview(rs.getString("order_no"))
            ), traceId());
        }, orderNo);
    }

    @GetMapping("/consumer/after-sales/detail")
    public ApiResponse<TwentyMallAfterSaleResponse> consumerAfterSaleDetail(@RequestParam String orderNo) {
        String sql = """
            SELECT a.id, a.after_sale_no, o.order_no, a.after_sale_type, a.reason_type, a.description,
                   a.requested_amount, a.status, a.created_at,
                   i.product_name, ma.display_name AS merchant_name
            FROM twenty_mall_after_sale a
            JOIN twenty_mall_order o ON o.id = a.order_id
            JOIN twenty_mall_order_item i ON i.id = a.order_item_id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            WHERE o.order_no = ? AND a.deleted = 0 AND o.deleted = 0 AND i.deleted = 0
            ORDER BY a.id DESC
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "该订单暂无有效售后申请", traceId());
            }
            AfterSaleDescription description = parseAfterSaleDescription(rs.getString("description"));
            return ApiResponse.success(new TwentyMallAfterSaleResponse(
                rs.getLong("id"),
                rs.getString("after_sale_no"),
                rs.getString("order_no"),
                rs.getString("after_sale_type"),
                rs.getString("reason_type"),
                rs.getBigDecimal("requested_amount"),
                rs.getString("status"),
                afterSalePriority(rs.getString("reason_type")),
                afterSaleReviewOpinion(rs.getString("status"), description.reason(), description.reviewReason()),
                "PENDING",
                formatTime(rs.getTimestamp("created_at")),
                rs.getString("product_name"),
                "TWENTY_MALL",
                "20商城",
                rs.getString("merchant_name"),
                description.reason(),
                description.evidenceImages()
            ), traceId());
        }, orderNo);
    }

    @PostMapping("/merchant/after-sales/review")
    public ApiResponse<TwentyMallAfterSaleResponse> reviewAfterSale(@Valid @RequestBody TwentyMallAfterSaleReviewRequest request) {
        String nextStatus = "REJECT".equals(request.result()) ? "REJECTED" : "PROCESSING";
        String orderStatus = "REJECT".equals(request.result()) ? "REJECTED" : "AFTER_SALE";
        String itemStatus = "REJECT".equals(request.result()) ? "REJECTED" : "APPLIED";
        if ("REJECT".equals(request.result())) {
            persistReviewReason(request.afterSaleId(), request.reason());
        }
        jdbcTemplate.update(
            "UPDATE twenty_mall_after_sale SET status = ?, updated_at = NOW() WHERE id = ? AND deleted = 0",
            nextStatus,
            request.afterSaleId()
        );
        jdbcTemplate.update(
            """
            UPDATE twenty_mall_order o
            JOIN twenty_mall_after_sale a ON a.order_id = o.id
            SET o.after_sale_status = ?, o.updated_at = NOW()
            WHERE a.id = ? AND a.deleted = 0
            """,
            orderStatus,
            request.afterSaleId()
        );
        jdbcTemplate.update(
            """
            UPDATE twenty_mall_order_item i
            JOIN twenty_mall_after_sale a ON a.order_item_id = i.id
            SET i.after_sale_status = ?, i.updated_at = NOW()
            WHERE a.id = ? AND a.deleted = 0
            """,
            itemStatus,
            request.afterSaleId()
        );
        return merchantAfterSaleById(request.afterSaleId());
    }

    @PostMapping("/consumer/reviews")
    public ApiResponse<Map<String, Long>> submitConsumerReview(@Valid @RequestBody TwentyMallReviewSubmitRequest request) {
        String sql = """
            SELECT o.id AS order_id, o.consumer_account_id, i.product_id
            FROM twenty_mall_order o
            JOIN twenty_mall_order_item i ON i.order_id = o.id
            WHERE o.order_no = ? AND o.deleted = 0 AND i.deleted = 0
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "订单不存在，无法评价", traceId());
            }
            List<Long> existing = jdbcTemplate.query(
                "SELECT id FROM twenty_mall_review WHERE order_id = ? ORDER BY id DESC LIMIT 1",
                (reviewRs, rowNum) -> reviewRs.getLong("id"),
                rs.getLong("order_id")
            );
            int productScore = normalizedScore(request.productScore(), request.score());
            int serviceScore = normalizedScore(request.serviceScore(), request.score());
            if (hasSplitReviewFields(request) && (isBlank(request.productContent()) || isBlank(request.merchantContent()))) {
                return ApiResponse.fail("400", "请分别填写产品质量评价和商家服务评价", traceId());
            }
            String content = buildReviewContent(request.productContent(), request.merchantContent(), request.content());
            if (!existing.isEmpty()) {
                return ApiResponse.fail("409", "该订单已评价，不能重复评价", traceId());
            }
            if (content.isBlank()) {
                return ApiResponse.fail("400", "请填写产品质量评价和商家服务评价", traceId());
            }
            jdbcTemplate.update(
                """
                INSERT INTO twenty_mall_review (
                  order_id, product_id, consumer_account_id, product_score, service_score, content, status, reviewed_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'PUBLISHED', NOW())
                """,
                rs.getLong("order_id"),
                rs.getLong("product_id"),
                rs.getLong("consumer_account_id"),
                productScore,
                serviceScore,
                content
            );
            long reviewId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            return ApiResponse.success(Map.of("reviewId", reviewId), traceId());
        }, request.orderNo());
    }

    private String traceId() {
        return String.valueOf(System.currentTimeMillis());
    }

    public record TwentyMallLoginRequest(
        @NotBlank(message = "账号不能为空")
        String accountNo,
        @NotBlank(message = "密码不能为空")
        String password,
        @NotBlank(message = "角色不能为空")
        String role
    ) {
    }

    public record TwentyMallBindRequest(
        @NotBlank(message = "账号不能为空")
        String accountNo,
        @NotBlank(message = "密码不能为空")
        String password,
        @NotBlank(message = "角色不能为空")
        String role,
        String primaryAccountNo,
        String primaryAccountType,
        String primaryDisplayName
    ) {
    }

    public record TwentyMallUnbindRequest(
        @NotBlank(message = "账号不能为空")
        String accountNo,
        @NotBlank(message = "角色不能为空")
        String role,
        @NotBlank(message = "一级账号不能为空")
        String primaryAccountNo,
        @NotBlank(message = "一级账号类型不能为空")
        String primaryAccountType
    ) {
    }

    public record TwentyMallPrimaryProfileSaveRequest(
        @NotBlank(message = "一级账号不能为空")
        String accountNo,
        @NotBlank(message = "一级账号类型不能为空")
        String accountType,
        String displayName,
        String avatar
    ) {
    }

    public record TwentyMallLoginResponse(
        String accountNo,
        String role,
        String displayName,
        String phone,
        String bindStatus
    ) {
    }

    public record TwentyMallPrimaryProfileResponse(
        String accountNo,
        String accountType,
        String displayName,
        String phone,
        String avatar,
        String loginMode,
        String status,
        Long bindingCount
    ) {
    }

    public record TwentyMallProfileResponse(
        String accountNo,
        String role,
        String displayName,
        String phone,
        String bindStatus,
        String avatar,
        String address
    ) {
    }

    public record TwentyMallAdminOverviewResponse(
        Long merchantCount,
        Long boundShopCount,
        Long todaySyncCount,
        Long pendingAfterSaleCount,
        Long processingAfterSaleCount,
        Long highRiskReviewCount,
        Long activeRuleCount,
        Long knowledgeCount,
        List<TwentyMallAdminTrendResponse> trendRows,
        List<TwentyMallAdminActivityResponse> activityRows
    ) {
    }

    public record TwentyMallAdminTrendResponse(
        String date,
        Long orderCount,
        Long afterSaleCount,
        Long reviewCount
    ) {
    }

    public record TwentyMallAdminActivityResponse(
        Long id,
        String module,
        String title,
        String content,
        String time
    ) {
    }

    public record TwentyMallAdminBindingOverviewResponse(
        List<TwentyMallAdminBindingResponse> consumerBindings,
        List<TwentyMallAdminBindingResponse> merchantBindings
    ) {
    }

    public record TwentyMallAdminSyncLogResponse(
        String task,
        String status,
        Long count,
        String time
    ) {
    }

    public record TwentyMallAdminBindingResponse(
        String primaryAccountNo,
        String primaryDisplayName,
        String primaryAvatar,
        String platformName,
        String secondaryAccountNo,
        String secondaryDisplayName,
        String bindStatus,
        String secondaryStatus,
        String boundAt
    ) {
    }

    public record TwentyMallAdminReviewResponse(
        Long id,
        String platform,
        String orderNo,
        String merchantName,
        String productName,
        Integer productScore,
        Integer serviceScore,
        Integer score,
        String content,
        String sentiment,
        String riskLevel,
        String keywords,
        String analysisSummary,
        String suggestion,
        String reviewedAt,
        Long disputeId,
        String disputeStatus,
        String disputeReason,
        String disputeAdminNote,
        String disputeCreatedAt
    ) {
    }

    public record TwentyMallAdminRuleResponse(
        Long id,
        String ruleName,
        String ruleType,
        String ruleTypeText,
        String conditionsJson,
        String conditionsText,
        String actionJson,
        String actionText,
        String content,
        Boolean enabled,
        String updatedAt
    ) {
    }

    public record TwentyMallAdminRuleSaveRequest(
        Long id,
        @NotBlank(message = "规则名称不能为空")
        String ruleName,
        @NotBlank(message = "规则类型不能为空")
        String ruleType,
        String conditionsJson,
        String actionJson,
        String content,
        Boolean enabled
    ) {
    }

    public record TwentyMallAdminRuleToggleRequest(
        Boolean enabled
    ) {
    }

    public record TwentyMallAdminKnowledgeOverviewResponse(
        List<TwentyMallAdminKnowledgeArticleResponse> articles,
        List<TwentyMallAdminFaqResponse> faqs
    ) {
    }

    public record TwentyMallAdminKnowledgeArticleResponse(
        Long id,
        String title,
        String content,
        String category,
        String categoryText,
        String tagsJson,
        String status,
        String statusText,
        String updatedAt
    ) {
    }

    public record TwentyMallAdminFaqResponse(
        Long id,
        String question,
        String answer,
        String category,
        String categoryText,
        Integer priority,
        Boolean enabled,
        String updatedAt
    ) {
    }

    public record TwentyMallAdminKnowledgeArticleSaveRequest(
        Long id,
        @NotBlank(message = "标题不能为空")
        String title,
        @NotBlank(message = "内容不能为空")
        String content,
        @NotBlank(message = "分类不能为空")
        String category,
        String tagsJson,
        @NotBlank(message = "状态不能为空")
        String status
    ) {
    }

    public record TwentyMallAdminFaqSaveRequest(
        Long id,
        @NotBlank(message = "问题不能为空")
        String question,
        @NotBlank(message = "答案不能为空")
        String answer,
        @NotBlank(message = "分类不能为空")
        String category,
        Integer priority,
        Boolean enabled
    ) {
    }

    public record TwentyMallAdminKnowledgeToggleRequest(
        Boolean enabled
    ) {
    }

    public record TwentyMallOrderResponse(
        String no,
        String title,
        String spec,
        String image,
        BigDecimal price,
        String status,
        String payStatus,
        String logisticsStatus,
        String afterSale,
        String merchant,
        String merchantAccountNo,
        String merchantPrimaryAccountNo,
        String orderedAt,
        String deliveredAt,
        List<String> policyTags,
        Boolean reviewed
    ) {
    }

    public record TwentyMallMerchantOrderResponse(
        Long id,
        String externalOrderNo,
        String buyerMaskedName,
        String orderStatus,
        String payStatus,
        String logisticsStatus,
        String afterSaleStatus,
        BigDecimal totalAmount,
        String orderedAt,
        String productName,
        String merchantName
    ) {
    }

    public record TwentyMallMerchantReviewResponse(
        Long id,
        String platformCode,
        String orderNo,
        String productName,
        String merchantName,
        Integer productScore,
        Integer serviceScore,
        String content,
        String sentiment,
        String riskLevel,
        String keywords,
        String analysisSummary,
        String suggestion,
        Long disputeId,
        String disputeStatus,
        String disputeReason,
        String disputeAdminNote,
        Boolean deleted
    ) {
    }

    public record TwentyMallMerchantNotificationResponse(
        String id,
        String type,
        String typeText,
        String title,
        String summary,
        String detail,
        Long relatedId,
        String relatedNo,
        String targetPath,
        String statusCode,
        String riskLevel,
        String statusText,
        String tone,
        String occurredAt
    ) {
    }

    public record TwentyMallReviewDisputeSubmitRequest(
        @NotBlank(message = "商家账号不能为空")
        String accountNo,
        @NotBlank(message = "异议原因不能为空")
        String reason
    ) {
    }

    public record TwentyMallReviewDisputeReviewRequest(
        @NotBlank(message = "审核结果不能为空")
        String result,
        String adminNote
    ) {
    }

    public record TwentyMallAfterSaleApplyRequest(
        @NotBlank(message = "订单号不能为空")
        String orderNo,
        @NotBlank(message = "售后类型不能为空")
        String afterSaleType,
        @NotBlank(message = "售后原因不能为空")
        String reasonType,
        String description,
        List<String> evidenceImages
    ) {
    }

    public record TwentyMallAfterSaleCancelRequest(
        @NotBlank(message = "订单号不能为空")
        String orderNo
    ) {
    }

    public record TwentyMallAfterSaleReviewRequest(
        Long afterSaleId,
        @NotBlank(message = "审核结果不能为空")
        String result,
        String reason
    ) {
    }

    public record TwentyMallReviewSubmitRequest(
        @NotBlank(message = "订单号不能为空")
        String orderNo,
        Integer score,
        Integer productScore,
        Integer serviceScore,
        String productContent,
        String merchantContent,
        String content
    ) {
    }

    public record TwentyMallAfterSaleResponse(
        Long id,
        String afterSaleNo,
        String orderNo,
        String afterSaleType,
        String reasonType,
        BigDecimal requestedAmount,
        String status,
        String priority,
        String reviewOpinion,
        String writeBackStatus,
        String createdAt,
        String productName,
        String platformCode,
        String platformName,
        String shopName,
        String description,
        List<String> evidenceImages
    ) {
    }

    private record AfterSaleDescription(
        String reason,
        List<String> evidenceImages,
        String reviewReason
    ) {
    }

    private String buildAfterSaleDescription(String reason, List<String> evidenceImages) {
        return buildAfterSaleDescription(reason, evidenceImages, "");
    }

    private String buildAfterSaleDescription(String reason, List<String> evidenceImages, String reviewReason) {
        try {
            return objectMapper.writeValueAsString(new AfterSaleDescription(reason, safeEvidenceImages(evidenceImages), reviewReason == null ? "" : reviewReason));
        } catch (JsonProcessingException e) {
            return reason;
        }
    }

    private AfterSaleDescription parseAfterSaleDescription(String raw) {
        if (raw == null || raw.isBlank()) {
            return new AfterSaleDescription("", List.of(), "");
        }
        if (!raw.trim().startsWith("{")) {
            return new AfterSaleDescription(raw, List.of(), "");
        }
        try {
            AfterSaleDescription payload = objectMapper.readValue(raw, AfterSaleDescription.class);
            return new AfterSaleDescription(
                payload.reason() == null ? "" : payload.reason(),
                safeEvidenceImages(payload.evidenceImages()),
                payload.reviewReason() == null ? "" : payload.reviewReason()
            );
        } catch (JsonProcessingException e) {
            return new AfterSaleDescription(raw, List.of(), "");
        }
    }

    private List<String> safeEvidenceImages(List<String> evidenceImages) {
        if (evidenceImages == null) {
            return List.of();
        }
        return evidenceImages.stream()
            .filter(item -> item != null && !item.isBlank())
            .limit(3)
            .toList();
    }

    private List<String> saveEvidenceImages(String afterSaleNo, List<String> evidenceImages) {
        if (evidenceImages == null || evidenceImages.isEmpty()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (int index = 0; index < evidenceImages.size() && urls.size() < 3; index++) {
            String image = evidenceImages.get(index);
            if (image == null || image.isBlank()) {
                continue;
            }
            if (image.startsWith("/api/twenty-mall/evidence/")) {
                urls.add(image);
                continue;
            }
            if (!image.startsWith("data:image/")) {
                continue;
            }
            String extension = image.startsWith("data:image/png") ? "png" : "jpg";
            int commaIndex = image.indexOf(',');
            if (commaIndex < 0) {
                continue;
            }
            try {
                Files.createDirectories(EVIDENCE_DIR);
                byte[] bytes = Base64.getDecoder().decode(image.substring(commaIndex + 1));
                String fileName = afterSaleNo + "-" + index + "-" + UUID.randomUUID() + "." + extension;
                Files.write(EVIDENCE_DIR.resolve(fileName), bytes);
                urls.add("/api/twenty-mall/evidence/" + fileName);
            } catch (IllegalArgumentException | IOException ignored) {
                // Ignore invalid demo evidence images so the after-sale request itself can still be submitted.
            }
        }
        return urls;
    }

    private String orderStatusText(String status) {
        return switch (status) {
            case "COMPLETED" -> "已完成";
            case "SHIPPED" -> "已发货";
            case "PENDING" -> "待处理";
            case "CANCELED" -> "已取消";
            case "PAID" -> "已支付";
            case "UNPAID" -> "待支付";
            case "REFUNDED" -> "已退款";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }

    private String payStatusText(String status) {
        return switch (status) {
            case "PAID" -> "已支付";
            case "UNPAID" -> "未支付";
            case "REFUNDED" -> "已退款";
            case "REFUNDING" -> "退款中";
            case "PART_REFUNDED" -> "部分退款";
            default -> status;
        };
    }

    private String logisticsStatusText(String status) {
        return switch (status) {
            case "RECEIVED", "SIGNED" -> "已签收";
            case "IN_TRANSIT" -> "运输中";
            case "WAITING", "WAIT_SHIPPING", "NOT_SHIPPED" -> "待发货";
            case "SHIPPED" -> "已发货";
            case "DELIVERED" -> "已送达";
            default -> status;
        };
    }

    private String afterSaleStatusText(String status) {
        return switch (status) {
            case "AFTER_SALE", "PROCESSING" -> "处理中";
            case "PENDING_REVIEW" -> "待审核";
            case "NONE" -> "未申请";
            case "COMPLETED" -> "已完成";
            case "APPLIED" -> "已申请";
            case "REJECTED" -> "已拒绝";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }

    private String bindStatusText(String status) {
        return switch (status) {
            case "BOUND", "ACTIVE" -> "已绑定";
            case "UNBOUND" -> "未绑定";
            case "EXPIRED" -> "已过期";
            case "DISABLED" -> "已停用";
            default -> status;
        };
    }

    private String disputeStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "PENDING" -> "待审核";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已拒绝";
            default -> "";
        };
    }

    private String notificationTypeText(String type) {
        return switch (type == null ? "" : type) {
            case "AFTER_SALE" -> "售后申请";
            case "REVIEW" -> "顾客评价";
            case "REVIEW_DISPUTE" -> "异议审核";
            default -> "系统通知";
        };
    }

    private String notificationSummaryText(String type, String summary) {
        if (summary == null || summary.isBlank()) {
            return "";
        }
        return summary
            .replace("PRODUCT_QUALITY", "商品质量问题")
            .replace("LOGISTICS_DELAY", "物流延迟")
            .replace("WRONG_GOODS", "商品错发")
            .replace("SIZE_MISMATCH", "尺码不符")
            .replace("NOT_AS_DESCRIBED", "描述不符")
            .replace("OTHER", "其他原因")
            .replace("PENDING_REVIEW", "待审核")
            .replace("PROCESSING", "处理中")
            .replace("APPROVED", "已通过")
            .replace("REJECTED", "已拒绝")
            .replace("COMPLETED", "已完成")
            .replace("CLOSED", "已关闭");
    }

    private String notificationDetailText(String type, String detail) {
        if (detail == null || detail.isBlank()) {
            return "暂无详细说明";
        }
        return notificationSummaryText(type, detail);
    }

    private String notificationStatusText(String type, String status) {
        if ("AFTER_SALE".equals(type)) {
            return afterSaleStatusText(status);
        }
        if ("REVIEW".equals(type)) {
            return riskLevelText(status);
        }
        if ("REVIEW_DISPUTE".equals(type)) {
            return disputeStatusText(status);
        }
        return status == null ? "" : status;
    }

    private String notificationTone(String type, String status) {
        if ("AFTER_SALE".equals(type)) {
            return "PENDING_REVIEW".equals(status) ? "warning" : "primary";
        }
        if ("REVIEW".equals(type)) {
            return "HIGH".equals(status) ? "danger" : "MEDIUM".equals(status) ? "warning" : "success";
        }
        if ("REVIEW_DISPUTE".equals(type)) {
            return "APPROVED".equals(status) ? "success" : "REJECTED".equals(status) ? "danger" : "warning";
        }
        return "info";
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return timestamp.toLocalDateTime().format(DISPLAY_TIME_FORMATTER);
    }

    private boolean shouldShowDeliveredAt(String orderStatus, String logisticsStatus) {
        return "COMPLETED".equals(orderStatus) || "RECEIVED".equals(logisticsStatus);
    }

    private List<String> parsePolicyTags(String policyTagsJson) {
        if (policyTagsJson == null || policyTagsJson.isBlank()) {
            return List.of();
        }
        String content = policyTagsJson.trim();
        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1);
        }
        if (content.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (String item : content.split(",")) {
            String tag = item.trim().replace("\"", "");
            if (!tag.isBlank()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private String reviewSentiment(int productScore, int serviceScore, String content) {
        if (productScore <= 2 || serviceScore <= 2 || containsAny(content, "异常", "破损", "质量差", "质量问题", "严重质量", "差", "不满意", "划痕")) {
            return "NEGATIVE";
        }
        if (productScore >= 4 && serviceScore >= 4) {
            return "POSITIVE";
        }
        return "NEUTRAL";
    }

    private String reviewRiskLevel(int productScore, int serviceScore, String content) {
        if (productScore <= 2 || containsAny(content, "严重", "投诉", "无法使用", "破损")) {
            return "HIGH";
        }
        if (productScore <= 3 || serviceScore <= 3 || containsAny(content, "异常", "质量", "划痕", "希望售后")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String reviewKeywords(String content, int productScore) {
        List<String> keywords = new ArrayList<>();
        if (containsAny(content, "空格键", "键盘", "回弹")) {
            keywords.add("键盘故障");
        }
        if (containsAny(content, "背包", "防水", "通勤", "收纳")) {
            keywords.add("背包体验");
        }
        if (containsAny(content, "客服", "售后")) {
            keywords.add("客服售后");
        }
        if (productScore <= 3) {
            keywords.add("商品体验");
        }
        if (keywords.isEmpty()) {
            keywords.add("正向反馈");
        }
        return String.join("、", keywords);
    }

    private String reviewSummary(String sentiment, String riskLevel, String content) {
        if ("HIGH".equals(riskLevel)) {
            return "评价存在高风险售后问题，需要优先跟进并形成处理记录。";
        }
        if ("MEDIUM".equals(riskLevel)) {
            return "评价包含商品体验问题，但服务反馈尚可，建议持续跟进。";
        }
        if ("POSITIVE".equals(sentiment)) {
            return "用户对商品或服务整体满意，可沉淀为正向服务样本。";
        }
        return content.length() > 28 ? content.substring(0, 28) + "..." : content;
    }

    private String reviewSuggestion(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH" -> "建议售后主管介入，24小时内联系用户并给出补偿或换货方案。";
            case "MEDIUM" -> "建议客服主动回访，核实问题并同步质检或售后处理进度。";
            default -> "可标记为低风险评价，用于服务质量复盘。";
        };
    }

    private String sentimentText(String sentiment) {
        return switch (sentiment) {
            case "NEGATIVE" -> "负向";
            case "POSITIVE" -> "正向";
            default -> "中性";
        };
    }

    private String riskLevelText(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH" -> "高风险";
            case "MEDIUM" -> "中风险";
            default -> "低风险";
        };
    }

    private String ruleTypeText(String ruleType) {
        return switch (ruleType) {
            case "RETURN_POLICY" -> "退货政策";
            case "REFUND_POLICY" -> "退款政策";
            case "PRICE_PROTECTION" -> "价保政策";
            case "FREIGHT_INSURANCE" -> "运费险政策";
            case "PRIORITY" -> "优先级规则";
            case "MANUAL_REVIEW" -> "人工审核规则";
            default -> ruleType == null || ruleType.isBlank() ? "未分类" : ruleType;
        };
    }

    private String knowledgeCategoryText(String category) {
        return switch (category) {
            case "PLATFORM_POLICY" -> "平台政策";
            case "PRODUCT_POLICY" -> "商品政策";
            case "AFTER_SALE_POLICY" -> "售后政策";
            case "SERVICE_SCRIPT" -> "客服话术";
            default -> category == null || category.isBlank() ? "未分类" : category;
        };
    }

    private String faqCategoryText(String category) {
        return switch (category) {
            case "REFUND" -> "退款问题";
            case "AFTER_SALE" -> "售后问题";
            case "RETURN" -> "退货问题";
            case "LOGISTICS" -> "物流问题";
            case "ACCOUNT" -> "账号问题";
            default -> category == null || category.isBlank() ? "未分类" : category;
        };
    }

    private String knowledgeStatusText(String status) {
        return switch (status) {
            case "PUBLISHED" -> "已发布";
            case "DRAFT" -> "草稿";
            case "DISABLED" -> "已停用";
            default -> status == null || status.isBlank() ? "未知" : status;
        };
    }

    private String normalizedJson(String rawJson) {
        String content = rawJson == null || rawJson.isBlank() ? "{}" : rawJson.trim();
        try {
            objectMapper.readTree(content);
            return content;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String ruleConditionText(String conditionsJson) {
        try {
            var node = objectMapper.readTree(conditionsJson == null || conditionsJson.isBlank() ? "{}" : conditionsJson);
            List<String> parts = new ArrayList<>();
            if (node.has("days")) {
                parts.add("签收后 " + node.get("days").asInt() + " 天内");
            }
            if (node.has("reasonType")) {
                parts.add("申请原因为" + reasonTypeText(node.get("reasonType").asText()));
            }
            if (node.has("orderStatus")) {
                parts.add("订单状态为" + orderStatusText(node.get("orderStatus").asText()));
            }
            if (node.has("logisticsStatus")) {
                parts.add("物流状态为" + logisticsStatusText(node.get("logisticsStatus").asText()));
            }
            if (node.has("productCategory")) {
                parts.add("商品类目为" + node.get("productCategory").asText());
            }
            return parts.isEmpty() ? "无特殊触发条件" : String.join("，", parts);
        } catch (JsonProcessingException e) {
            return conditionsJson == null || conditionsJson.isBlank() ? "无特殊触发条件" : conditionsJson;
        }
    }

    private String ruleActionText(String actionJson) {
        try {
            var node = objectMapper.readTree(actionJson == null || actionJson.isBlank() ? "{}" : actionJson);
            List<String> parts = new ArrayList<>();
            if (node.has("allowReturn")) {
                parts.add(node.get("allowReturn").asBoolean() ? "允许退货" : "不允许退货");
            }
            if (node.has("allowRefund")) {
                parts.add(node.get("allowRefund").asBoolean() ? "允许退款" : "不允许退款");
            }
            if (node.has("priority")) {
                parts.add("优先级设为" + priorityText(node.get("priority").asText()));
            }
            if (node.has("needManualReview")) {
                parts.add(node.get("needManualReview").asBoolean() ? "需要人工审核" : "无需人工审核");
            }
            if (node.has("priceProtectDays")) {
                parts.add("支持 " + node.get("priceProtectDays").asInt() + " 天价保");
            }
            return parts.isEmpty() ? "记录规则命中结果" : String.join("，", parts);
        } catch (JsonProcessingException e) {
            return actionJson == null || actionJson.isBlank() ? "记录规则命中结果" : actionJson;
        }
    }

    private String reasonTypeText(String reasonType) {
        return switch (reasonType) {
            case "PRODUCT_QUALITY" -> "商品质量问题";
            case "WRONG_GOODS" -> "错发或漏发";
            case "NO_REASON" -> "七天无理由";
            case "PRICE_PROTECTION" -> "价格保护";
            default -> reasonType == null || reasonType.isBlank() ? "未指定原因" : reasonType;
        };
    }

    private String priorityText(String priority) {
        return switch (priority) {
            case "HIGH" -> "高";
            case "NORMAL" -> "普通";
            case "LOW" -> "低";
            default -> priority == null || priority.isBlank() ? "普通" : priority;
        };
    }

    private String cleanProductName(String productName) {
        if (productName == null) {
            return "";
        }
        return productName.replaceFirst("^20商城\\s*", "").trim();
    }

    private String existingAfterSaleNo(long orderId, long itemId) {
        List<String> rows = jdbcTemplate.query(
            "SELECT after_sale_no FROM twenty_mall_after_sale WHERE order_id = ? AND order_item_id = ? AND deleted = 0 ORDER BY id DESC LIMIT 1",
            (rs, rowNum) -> rs.getString("after_sale_no"),
            orderId,
            itemId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Long afterSaleId(String afterSaleNo) {
        List<Long> rows = jdbcTemplate.query(
            "SELECT id FROM twenty_mall_after_sale WHERE after_sale_no = ? LIMIT 1",
            (rs, rowNum) -> rs.getLong("id"),
            afterSaleNo
        );
        return rows.isEmpty() ? 0L : rows.get(0);
    }

    private String effectiveAfterSaleStatus(String orderNo, String fallbackStatus) {
        List<String> rows = jdbcTemplate.query(
            """
            SELECT a.status
            FROM twenty_mall_after_sale a
            JOIN twenty_mall_order o ON o.id = a.order_id
            WHERE o.order_no = ? AND a.deleted = 0
            ORDER BY a.id DESC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getString("status"),
            orderNo
        );
        if (rows.isEmpty()) {
            return "NONE";
        }
        return rows.get(0) == null ? fallbackStatus : rows.get(0);
    }

    private boolean hasPublishedReview(String orderNo) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(1)
            FROM twenty_mall_review r
            JOIN twenty_mall_order o ON o.id = r.order_id
            WHERE o.order_no = ?
            """,
            Integer.class,
            orderNo
        );
        return count != null && count > 0;
    }

    private int normalizedScore(Integer score, Integer fallbackScore) {
        int rawScore = score == null ? (fallbackScore == null ? 5 : fallbackScore) : score;
        return Math.max(1, Math.min(5, rawScore));
    }

    private boolean hasSplitReviewFields(TwentyMallReviewSubmitRequest request) {
        return request.productScore() != null
            || request.serviceScore() != null
            || !isBlank(request.productContent())
            || !isBlank(request.merchantContent());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private String buildReviewContent(String productContent, String merchantContent, String fallbackContent) {
        String product = productContent == null ? "" : productContent.trim();
        String merchant = merchantContent == null ? "" : merchantContent.trim();
        if (product.isBlank() && merchant.isBlank()) {
            return fallbackContent == null ? "" : fallbackContent.trim();
        }
        return "产品质量评价：" + (product.isBlank() ? "未填写" : product)
            + "\n商家服务评价：" + (merchant.isBlank() ? "未填写" : merchant);
    }

    private void persistReviewReason(Long afterSaleId, String reviewReason) {
        List<String> rows = jdbcTemplate.query(
            "SELECT description FROM twenty_mall_after_sale WHERE id = ? AND deleted = 0 LIMIT 1",
            (rs, rowNum) -> rs.getString("description"),
            afterSaleId
        );
        if (rows.isEmpty()) {
            return;
        }
        AfterSaleDescription description = parseAfterSaleDescription(rows.get(0));
        jdbcTemplate.update(
            "UPDATE twenty_mall_after_sale SET description = ?, updated_at = NOW() WHERE id = ? AND deleted = 0",
            buildAfterSaleDescription(description.reason(), description.evidenceImages(), reviewReason),
            afterSaleId
        );
    }

    private ApiResponse<TwentyMallAfterSaleResponse> merchantAfterSaleById(Long afterSaleId) {
        String sql = """
            SELECT a.id, a.after_sale_no, o.order_no, a.after_sale_type, a.reason_type, a.description,
                   a.requested_amount, a.status, a.created_at,
                   i.product_name, ma.display_name AS merchant_name
            FROM twenty_mall_after_sale a
            JOIN twenty_mall_order o ON o.id = a.order_id
            JOIN twenty_mall_order_item i ON i.id = a.order_item_id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            WHERE a.id = ? AND a.deleted = 0 AND o.deleted = 0 AND i.deleted = 0
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return ApiResponse.fail("404", "售后申请不存在", traceId());
            }
            AfterSaleDescription description = parseAfterSaleDescription(rs.getString("description"));
            return ApiResponse.success(new TwentyMallAfterSaleResponse(
                rs.getLong("id"),
                rs.getString("after_sale_no"),
                rs.getString("order_no"),
                rs.getString("after_sale_type"),
                rs.getString("reason_type"),
                rs.getBigDecimal("requested_amount"),
                rs.getString("status"),
                afterSalePriority(rs.getString("reason_type")),
                afterSaleReviewOpinion(rs.getString("status"), description.reason(), description.reviewReason()),
                "PENDING",
                formatTime(rs.getTimestamp("created_at")),
                rs.getString("product_name"),
                "TWENTY_MALL",
                "20商城",
                rs.getString("merchant_name"),
                description.reason(),
                description.evidenceImages()
            ), traceId());
        }, afterSaleId);
    }

    private String afterSalePriority(String reasonType) {
        return switch (reasonType) {
            case "PRODUCT_QUALITY", "WRONG_GOODS" -> "HIGH";
            default -> "NORMAL";
        };
    }

    private String afterSaleReviewOpinion(String status, String description) {
        return afterSaleReviewOpinion(status, description, "");
    }

    private String afterSaleReviewOpinion(String status, String description, String reviewReason) {
        if ("PENDING_REVIEW".equals(status)) {
            return "待商家审核";
        }
        if ("REJECTED".equals(status)) {
            if (reviewReason != null && !reviewReason.isBlank()) {
                return "审核拒绝：" + reviewReason;
            }
            return "商家已拒绝售后申请";
        }
        if ("COMPLETED".equals(status)) {
            return "售后已处理完成";
        }
        return description == null || description.isBlank() ? "已进入人工复核" : "已进入人工复核：" + description;
    }

    private boolean containsAny(String content, String... words) {
        if (content == null) {
            return false;
        }
        for (String word : words) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String profileAddress(String accountNo) {
        if ("20230141".equals(accountNo)) {
            return "重庆市沙坪坝区大学城20商城学生公寓 41号";
        }
        return "四川省成都市高新区20商城模拟社区 2023号";
    }
}
