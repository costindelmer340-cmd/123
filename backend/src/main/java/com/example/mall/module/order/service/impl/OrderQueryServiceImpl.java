package com.example.mall.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.auth.SecurityUtils;
import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.order.dto.LogisticsSnapshotResponse;
import com.example.mall.module.order.dto.OrderDetailResponse;
import com.example.mall.module.order.dto.OrderItemResponse;
import com.example.mall.module.order.dto.OrderSummaryResponse;
import com.example.mall.module.order.dto.PaymentSnapshotResponse;
import com.example.mall.module.order.entity.ExternalLogisticsSnapshot;
import com.example.mall.module.order.entity.ExternalOrder;
import com.example.mall.module.order.entity.ExternalOrderItem;
import com.example.mall.module.order.entity.ExternalPaymentSnapshot;
import com.example.mall.module.order.mapper.ExternalLogisticsSnapshotMapper;
import com.example.mall.module.order.mapper.ExternalOrderItemMapper;
import com.example.mall.module.order.mapper.ExternalOrderMapper;
import com.example.mall.module.order.mapper.ExternalPaymentSnapshotMapper;
import com.example.mall.module.order.service.OrderQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    private final ExternalOrderMapper externalOrderMapper;
    private final ExternalOrderItemMapper externalOrderItemMapper;
    private final ExternalPaymentSnapshotMapper externalPaymentSnapshotMapper;
    private final ExternalLogisticsSnapshotMapper externalLogisticsSnapshotMapper;

    public OrderQueryServiceImpl(
        ExternalOrderMapper externalOrderMapper,
        ExternalOrderItemMapper externalOrderItemMapper,
        ExternalPaymentSnapshotMapper externalPaymentSnapshotMapper,
        ExternalLogisticsSnapshotMapper externalLogisticsSnapshotMapper
    ) {
        this.externalOrderMapper = externalOrderMapper;
        this.externalOrderItemMapper = externalOrderItemMapper;
        this.externalPaymentSnapshotMapper = externalPaymentSnapshotMapper;
        this.externalLogisticsSnapshotMapper = externalLogisticsSnapshotMapper;
    }

    @Override
    public PageResponse<OrderSummaryResponse> pageMerchantOrders(
        long pageNum,
        long pageSize,
        String platformCode,
        String orderStatus,
        String keyword
    ) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Page<ExternalOrder> page = externalOrderMapper.selectPage(
            new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
            new LambdaQueryWrapper<ExternalOrder>()
                .eq(ExternalOrder::getMerchantId, merchantId)
                .eq(platformCode != null && !platformCode.isBlank(), ExternalOrder::getPlatformCode, platformCode)
                .eq(orderStatus != null && !orderStatus.isBlank(), ExternalOrder::getOrderStatus, orderStatus)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                    .like(ExternalOrder::getExternalOrderNo, keyword)
                    .or()
                    .like(ExternalOrder::getBuyerMaskedName, keyword)
                    .or()
                    .like(ExternalOrder::getBuyerMaskedPhone, keyword)
                )
                .orderByDesc(ExternalOrder::getOrderedAt)
                .orderByDesc(ExternalOrder::getId)
        );
        List<OrderSummaryResponse> records = page.getRecords().stream().map(this::toSummaryResponse).toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    public OrderDetailResponse merchantOrderDetail(Long orderId) {
        Long merchantId = SecurityUtils.currentMerchantId();
        ExternalOrder order = externalOrderMapper.selectById(orderId);
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "Order not found");
        }
        List<OrderItemResponse> items = externalOrderItemMapper.selectList(
            new LambdaQueryWrapper<ExternalOrderItem>()
                .eq(ExternalOrderItem::getExternalOrderId, orderId)
                .orderByAsc(ExternalOrderItem::getId)
        ).stream().map(this::toItemResponse).toList();
        List<PaymentSnapshotResponse> payments = externalPaymentSnapshotMapper.selectList(
            new LambdaQueryWrapper<ExternalPaymentSnapshot>()
                .eq(ExternalPaymentSnapshot::getExternalOrderId, orderId)
                .orderByDesc(ExternalPaymentSnapshot::getPaidAt)
        ).stream().map(this::toPaymentResponse).toList();
        List<LogisticsSnapshotResponse> logistics = externalLogisticsSnapshotMapper.selectList(
            new LambdaQueryWrapper<ExternalLogisticsSnapshot>()
                .eq(ExternalLogisticsSnapshot::getExternalOrderId, orderId)
                .orderByDesc(ExternalLogisticsSnapshot::getShippedAt)
        ).stream().map(this::toLogisticsResponse).toList();
        return new OrderDetailResponse(toSummaryResponse(order), items, payments, logistics);
    }

    private OrderSummaryResponse toSummaryResponse(ExternalOrder order) {
        return new OrderSummaryResponse(
            order.getId(),
            order.getShopBindingId(),
            order.getPlatformCode(),
            order.getExternalOrderNo(),
            order.getBuyerMaskedName(),
            order.getBuyerMaskedPhone(),
            order.getOrderStatus(),
            order.getPayStatus(),
            order.getLogisticsStatus(),
            order.getAfterSaleStatus(),
            order.getTotalAmount(),
            order.getPayableAmount(),
            order.getPaidAt(),
            order.getOrderedAt(),
            order.getCompletedAt(),
            order.getLastSyncedAt()
        );
    }

    private OrderItemResponse toItemResponse(ExternalOrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getExternalItemId(),
            item.getExternalProductId(),
            item.getProductName(),
            item.getSkuName(),
            item.getProductImageUrl(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getTotalAmount(),
            item.getAfterSaleStatus()
        );
    }

    private PaymentSnapshotResponse toPaymentResponse(ExternalPaymentSnapshot payment) {
        return new PaymentSnapshotResponse(
            payment.getId(),
            payment.getExternalPaymentNo(),
            payment.getPayChannel(),
            payment.getPayStatus(),
            payment.getPaidAmount(),
            payment.getPaidAt(),
            payment.getLastSyncedAt()
        );
    }

    private LogisticsSnapshotResponse toLogisticsResponse(ExternalLogisticsSnapshot logistics) {
        return new LogisticsSnapshotResponse(
            logistics.getId(),
            logistics.getLogisticsCompany(),
            logistics.getTrackingNo(),
            logistics.getLogisticsStatus(),
            logistics.getShippedAt(),
            logistics.getReceivedAt(),
            logistics.getTrackingDetail(),
            logistics.getLastSyncedAt()
        );
    }
}
