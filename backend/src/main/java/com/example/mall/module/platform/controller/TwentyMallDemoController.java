package com.example.mall.module.platform.controller;

import com.example.mall.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
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

    public TwentyMallDemoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
                   o.total_amount, i.product_name, i.sku_name, i.product_image_url
            FROM twenty_mall_order o
            JOIN twenty_mall_account a ON a.id = o.consumer_account_id
            JOIN twenty_mall_order_item i ON i.order_id = o.id
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
            afterSaleStatusText(rs.getString("after_sale_status"))
        ), accountNo), traceId());
    }

    @GetMapping("/consumer/orders/detail")
    public ApiResponse<TwentyMallOrderResponse> orderDetail(@RequestParam String orderNo) {
        String sql = """
            SELECT o.order_no, o.order_status, o.pay_status, o.logistics_status, o.after_sale_status,
                   o.total_amount, i.product_name, i.sku_name, i.product_image_url
            FROM twenty_mall_order o
            JOIN twenty_mall_order_item i ON i.order_id = o.id
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
                afterSaleStatusText(rs.getString("after_sale_status"))
            ), traceId());
        }, orderNo);
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
        String afterSale
    ) {
    }

    private String orderStatusText(String status) {
        return switch (status) {
            case "COMPLETED" -> "已完成";
            case "SHIPPED" -> "已发货";
            default -> status;
        };
    }

    private String payStatusText(String status) {
        return "PAID".equals(status) ? "已支付" : status;
    }

    private String logisticsStatusText(String status) {
        if ("RECEIVED".equals(status)) {
            return "已签收";
        }
        if ("IN_TRANSIT".equals(status)) {
            return "运输中";
        }
        return status;
    }

    private String afterSaleStatusText(String status) {
        if ("AFTER_SALE".equals(status)) {
            return "处理中";
        }
        if ("NONE".equals(status)) {
            return "未申请";
        }
        return status;
    }

    private String profileAddress(String accountNo) {
        if ("20230141".equals(accountNo)) {
            return "重庆市沙坪坝区大学城20商城学生公寓 41号";
        }
        return "四川省成都市高新区20商城模拟社区 2023号";
    }
}
