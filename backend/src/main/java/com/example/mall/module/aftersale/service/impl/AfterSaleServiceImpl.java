package com.example.mall.module.aftersale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.auth.LoginUser;
import com.example.mall.common.auth.SecurityUtils;
import com.example.mall.common.exception.BusinessException;
import com.example.mall.common.exception.ErrorCode;
import com.example.mall.common.response.PageResponse;
import com.example.mall.module.aftersale.dto.AfterSaleDetailResponse;
import com.example.mall.module.aftersale.dto.AfterSaleMaterialResponse;
import com.example.mall.module.aftersale.dto.AfterSaleSummaryResponse;
import com.example.mall.module.aftersale.dto.CreateAfterSaleRequest;
import com.example.mall.module.aftersale.dto.ExternalAfterSaleMappingResponse;
import com.example.mall.module.aftersale.dto.RefundRecordResponse;
import com.example.mall.module.aftersale.dto.ReviewAfterSaleRequest;
import com.example.mall.module.aftersale.entity.AfterSaleApplication;
import com.example.mall.module.aftersale.entity.AfterSaleMaterial;
import com.example.mall.module.aftersale.entity.AfterSaleWriteBackLog;
import com.example.mall.module.aftersale.entity.ExternalAfterSaleMapping;
import com.example.mall.module.aftersale.entity.RefundRecord;
import com.example.mall.module.aftersale.mapper.AfterSaleApplicationMapper;
import com.example.mall.module.aftersale.mapper.AfterSaleMaterialMapper;
import com.example.mall.module.aftersale.mapper.AfterSaleWriteBackLogMapper;
import com.example.mall.module.aftersale.mapper.ExternalAfterSaleMappingMapper;
import com.example.mall.module.aftersale.mapper.RefundRecordMapper;
import com.example.mall.module.order.entity.ExternalOrder;
import com.example.mall.module.order.mapper.ExternalOrderMapper;
import com.example.mall.module.aftersale.service.AfterSaleService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AfterSaleServiceImpl implements AfterSaleService {

    private final AfterSaleApplicationMapper afterSaleApplicationMapper;
    private final AfterSaleMaterialMapper afterSaleMaterialMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final ExternalAfterSaleMappingMapper externalAfterSaleMappingMapper;
    private final AfterSaleWriteBackLogMapper afterSaleWriteBackLogMapper;
    private final ExternalOrderMapper externalOrderMapper;

    public AfterSaleServiceImpl(
        AfterSaleApplicationMapper afterSaleApplicationMapper,
        AfterSaleMaterialMapper afterSaleMaterialMapper,
        RefundRecordMapper refundRecordMapper,
        ExternalAfterSaleMappingMapper externalAfterSaleMappingMapper,
        AfterSaleWriteBackLogMapper afterSaleWriteBackLogMapper,
        ExternalOrderMapper externalOrderMapper
    ) {
        this.afterSaleApplicationMapper = afterSaleApplicationMapper;
        this.afterSaleMaterialMapper = afterSaleMaterialMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.externalAfterSaleMappingMapper = externalAfterSaleMappingMapper;
        this.afterSaleWriteBackLogMapper = afterSaleWriteBackLogMapper;
        this.externalOrderMapper = externalOrderMapper;
    }

    @Override
    public PageResponse<AfterSaleSummaryResponse> pageMerchantAfterSales(long pageNum, long pageSize, String status, String keyword) {
        Long merchantId = SecurityUtils.currentMerchantId();
        Page<AfterSaleApplication> page = afterSaleApplicationMapper.selectPage(
            new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
            new LambdaQueryWrapper<AfterSaleApplication>()
                .eq(AfterSaleApplication::getMerchantId, merchantId)
                .eq(status != null && !status.isBlank(), AfterSaleApplication::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                    .like(AfterSaleApplication::getAfterSaleNo, keyword)
                    .or()
                    .like(AfterSaleApplication::getReasonType, keyword)
                    .or()
                    .like(AfterSaleApplication::getProblemDescription, keyword)
                )
                .orderByDesc(AfterSaleApplication::getCreatedAt)
        );
        List<AfterSaleSummaryResponse> records = page.getRecords().stream().map(this::toSummaryResponse).toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    public AfterSaleDetailResponse afterSaleDetail(Long afterSaleId) {
        AfterSaleApplication application = requireMerchantAfterSale(afterSaleId);
        List<AfterSaleMaterialResponse> materials = afterSaleMaterialMapper.selectList(
            new LambdaQueryWrapper<AfterSaleMaterial>()
                .eq(AfterSaleMaterial::getAfterSaleId, afterSaleId)
                .orderByAsc(AfterSaleMaterial::getId)
        ).stream().map(this::toMaterialResponse).toList();
        List<RefundRecordResponse> refunds = refundRecordMapper.selectList(
            new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getAfterSaleId, afterSaleId)
                .orderByDesc(RefundRecord::getCreatedAt)
        ).stream().map(this::toRefundResponse).toList();
        List<ExternalAfterSaleMappingResponse> mappings = externalAfterSaleMappingMapper.selectList(
            new LambdaQueryWrapper<ExternalAfterSaleMapping>()
                .eq(ExternalAfterSaleMapping::getAfterSaleId, afterSaleId)
                .orderByDesc(ExternalAfterSaleMapping::getCreatedAt)
        ).stream().map(this::toMappingResponse).toList();
        return new AfterSaleDetailResponse(toSummaryResponse(application), materials, refunds, mappings);
    }

    @Override
    @Transactional
    public AfterSaleSummaryResponse createConsumerAfterSale(CreateAfterSaleRequest request) {
        LoginUser currentUser = SecurityUtils.currentUser();
        ExternalOrder order = externalOrderMapper.selectById(request.externalOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "External order not found");
        }
        AfterSaleApplication application = new AfterSaleApplication();
        application.setAfterSaleNo("AS" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(Locale.ROOT));
        application.setExternalOrderId(order.getId());
        application.setExternalOrderItemId(request.externalOrderItemId());
        application.setUserId(currentUser.getUserId());
        application.setMerchantId(order.getMerchantId());
        application.setShopBindingId(order.getShopBindingId());
        application.setAfterSaleType(request.afterSaleType());
        application.setReasonType(request.reasonType());
        application.setProblemDescription(request.problemDescription());
        application.setRequestedAmount(request.requestedAmount());
        application.setStatus("PENDING_REVIEW");
        application.setPriority("NORMAL");
        afterSaleApplicationMapper.insert(application);
        return toSummaryResponse(application);
    }

    @Override
    @Transactional
    public AfterSaleSummaryResponse reviewAfterSale(Long afterSaleId, ReviewAfterSaleRequest request) {
        AfterSaleApplication application = requireMerchantAfterSale(afterSaleId);
        String status = normalizeStatus(request.status());
        application.setStatus(status);
        application.setReviewOpinion(request.reviewOpinion());
        application.setFinalResult(request.finalResult());
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewerId(SecurityUtils.currentUser().getUserId());
        afterSaleApplicationMapper.updateById(application);

        if ("APPROVED".equals(status) || "PROCESSING".equals(status)) {
            createOrUpdateRefund(application, request);
            createOrUpdateMapping(application, "SELLER_APPROVED");
        } else if ("REJECTED".equals(status)) {
            createOrUpdateMapping(application, "SELLER_REJECTED");
        }
        writeBackLog(application, status, request);
        return toSummaryResponse(application);
    }

    private AfterSaleApplication requireMerchantAfterSale(Long afterSaleId) {
        Long merchantId = SecurityUtils.currentMerchantId();
        AfterSaleApplication application = afterSaleApplicationMapper.selectById(afterSaleId);
        if (application == null || !merchantId.equals(application.getMerchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "After-sale application not found");
        }
        return application;
    }

    private void createOrUpdateRefund(AfterSaleApplication application, ReviewAfterSaleRequest request) {
        RefundRecord refundRecord = refundRecordMapper.selectOne(
            new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getAfterSaleId, application.getId())
                .last("limit 1")
        );
        if (refundRecord == null) {
            refundRecord = new RefundRecord();
            refundRecord.setRefundNo("RF" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(Locale.ROOT));
            refundRecord.setAfterSaleId(application.getId());
            refundRecord.setExternalOrderId(application.getExternalOrderId());
            refundRecord.setMerchantId(application.getMerchantId());
            refundRecord.setPlatformCode("DOUYIN");
            refundRecord.setRefundAmount(application.getRequestedAmount());
            refundRecord.setRefundStatus("PROCESSING");
            refundRecord.setReason(application.getReasonType());
            refundRecord.setRawData("{\"source\":\"merchant-review\"}");
            refundRecordMapper.insert(refundRecord);
            return;
        }
        refundRecord.setRefundStatus("PROCESSING");
        refundRecord.setReason(request.reviewOpinion());
        refundRecordMapper.updateById(refundRecord);
    }

    private void createOrUpdateMapping(AfterSaleApplication application, String externalStatus) {
        ExternalAfterSaleMapping mapping = externalAfterSaleMappingMapper.selectOne(
            new LambdaQueryWrapper<ExternalAfterSaleMapping>()
                .eq(ExternalAfterSaleMapping::getAfterSaleId, application.getId())
                .last("limit 1")
        );
        if (mapping == null) {
            mapping = new ExternalAfterSaleMapping();
            mapping.setAfterSaleId(application.getId());
            mapping.setPlatformCode("DOUYIN");
            mapping.setExternalAfterSaleNo("DY" + application.getAfterSaleNo());
            mapping.setExternalRefundNo("DYRF" + application.getAfterSaleNo());
            mapping.setExternalStatus(externalStatus);
            mapping.setRawData("{\"source\":\"merchant-review\"}");
            externalAfterSaleMappingMapper.insert(mapping);
            return;
        }
        mapping.setExternalStatus(externalStatus);
        externalAfterSaleMappingMapper.updateById(mapping);
    }

    private void writeBackLog(AfterSaleApplication application, String status, ReviewAfterSaleRequest request) {
        AfterSaleWriteBackLog log = new AfterSaleWriteBackLog();
        log.setAfterSaleId(application.getId());
        log.setPlatformCode("DOUYIN");
        log.setShopBindingId(application.getShopBindingId());
        log.setActionType(status);
        log.setRequestPayload("{\"status\":\"" + status + "\"}");
        log.setResponsePayload("{\"result\":\"ok\"}");
        log.setStatus("SUCCESS");
        log.setRetryCount(0);
        log.setCreatedBy(SecurityUtils.currentUser().getUserId());
        afterSaleWriteBackLogMapper.insert(log);
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED", "PROCESSING", "CLOSED").contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "Unsupported after-sale status: " + status);
        }
        return normalized;
    }

    private AfterSaleSummaryResponse toSummaryResponse(AfterSaleApplication application) {
        return new AfterSaleSummaryResponse(
            application.getId(),
            application.getAfterSaleNo(),
            application.getExternalOrderId(),
            application.getExternalOrderItemId(),
            application.getUserId(),
            application.getMerchantId(),
            application.getAfterSaleType(),
            application.getReasonType(),
            application.getRequestedAmount(),
            application.getStatus(),
            application.getPriority(),
            application.getAiCategory(),
            application.getCreatedAt()
        );
    }

    private AfterSaleMaterialResponse toMaterialResponse(AfterSaleMaterial material) {
        return new AfterSaleMaterialResponse(
            material.getId(),
            material.getUserId(),
            material.getMaterialType(),
            material.getMaterialUrl(),
            material.getDescription(),
            material.getCreatedAt()
        );
    }

    private RefundRecordResponse toRefundResponse(RefundRecord record) {
        return new RefundRecordResponse(
            record.getId(),
            record.getRefundNo(),
            record.getPlatformCode(),
            record.getExternalRefundNo(),
            record.getRefundAmount(),
            record.getRefundStatus(),
            record.getReason(),
            record.getRefundedAt()
        );
    }

    private ExternalAfterSaleMappingResponse toMappingResponse(ExternalAfterSaleMapping mapping) {
        return new ExternalAfterSaleMappingResponse(
            mapping.getId(),
            mapping.getPlatformCode(),
            mapping.getExternalAfterSaleNo(),
            mapping.getExternalRefundNo(),
            mapping.getExternalStatus(),
            mapping.getLastSyncedAt()
        );
    }
}
