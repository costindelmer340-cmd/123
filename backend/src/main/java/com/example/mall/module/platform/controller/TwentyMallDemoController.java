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
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.M.d HH:mm:ss");
    private static final Path EVIDENCE_DIR = Paths.get("target", "demo-evidence");

    public TwentyMallDemoController(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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
    public ApiResponse<Map<String, String>> bind(@Valid @RequestBody TwentyMallLoginRequest request) {
        ApiResponse<TwentyMallLoginResponse> loginResult = login(request);
        if (!"200".equals(loginResult.code())) {
            return ApiResponse.fail(loginResult.code(), loginResult.message(), traceId());
        }
        jdbcTemplate.update(
            "UPDATE twenty_mall_account SET bind_status = 'BOUND', updated_at = NOW() WHERE account_no = ? AND account_role = ?",
            request.accountNo(),
            request.role()
        );
        return ApiResponse.success(Map.of("bindStatus", "BOUND"), traceId());
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
                   ma.display_name AS merchant_name
            FROM twenty_mall_order o
            JOIN twenty_mall_account a ON a.id = o.consumer_account_id
            JOIN twenty_mall_order_item i ON i.order_id = o.id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
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
            afterSaleStatusText(rs.getString("after_sale_status")),
            rs.getBigDecimal("total_amount"),
            formatTime(rs.getTimestamp("ordered_at")),
            rs.getString("product_name"),
            rs.getString("merchant_name")
        ), accountNo), traceId());
    }

    @GetMapping("/merchant/reviews")
    public ApiResponse<List<TwentyMallMerchantReviewResponse>> merchantReviews(@RequestParam String accountNo) {
        String sql = """
            SELECT r.id, r.product_score, r.service_score, r.content, r.reviewed_at,
                   o.order_no, p.product_name, ma.display_name AS merchant_name
            FROM twenty_mall_review r
            JOIN twenty_mall_order o ON o.id = r.order_id
            JOIN twenty_mall_product p ON p.id = r.product_id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
            WHERE ma.account_no = ? AND ma.account_role = 'MERCHANT' AND r.deleted = 0 AND o.deleted = 0
            ORDER BY r.reviewed_at DESC, r.id DESC
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
                reviewSuggestion(riskLevel)
            );
        }, accountNo), traceId());
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
                   ma.display_name AS merchant_name
            FROM twenty_mall_order o
            JOIN twenty_mall_order_item i ON i.order_id = o.id
            JOIN twenty_mall_account ma ON ma.id = o.merchant_account_id
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
                "SELECT id FROM twenty_mall_review WHERE order_id = ? AND deleted = 0 ORDER BY id DESC LIMIT 1",
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

    public record TwentyMallLoginResponse(
        String accountNo,
        String role,
        String displayName,
        String phone,
        String bindStatus
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
        String suggestion
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
        if (productScore <= 2 || serviceScore <= 2 || containsAny(content, "异常", "破损", "质量", "差", "不满意", "划痕")) {
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
            WHERE o.order_no = ? AND r.deleted = 0
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
