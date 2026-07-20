package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.enums.DomainEventType;
import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.MerchantConfigurationRequest;
import com.paymentprocessor.merchantservice.dto.MerchantConfigurationResponse;
import com.paymentprocessor.merchantservice.entity.MerchantConfiguration;
import com.paymentprocessor.merchantservice.event.OutboxWriter;
import com.paymentprocessor.merchantservice.repository.MerchantConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Manages centralized merchant operational configuration and emits configuration-updated events. */
@Service
public class MerchantConfigurationService {

    private final MerchantConfigurationRepository repository;
    private final MerchantService merchantService;
    private final OutboxWriter outbox;

    public MerchantConfigurationService(MerchantConfigurationRepository repository,
                                        MerchantService merchantService, OutboxWriter outbox) {
        this.repository = repository;
        this.merchantService = merchantService;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public MerchantConfigurationResponse get(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return toResponse(load(merchantId));
    }

    @Transactional
    public MerchantConfigurationResponse update(UUID merchantId, MerchantConfigurationRequest req) {
        merchantService.assertAccessible(merchantId);
        MerchantConfiguration c = load(merchantId);
        if (req.captureMode() != null) c.setCaptureMode(req.captureMode());
        if (req.enforce3ds() != null) c.setEnforce3ds(req.enforce3ds());
        if (req.velocityLimitPerDay() != null) c.setVelocityLimitPerDay(req.velocityLimitPerDay());
        if (req.payoutSchedule() != null) c.setPayoutSchedule(req.payoutSchedule());
        if (req.minimumPayoutThreshold() != null) c.setMinimumPayoutThreshold(req.minimumPayoutThreshold());
        if (req.instantPayoutEligible() != null) c.setInstantPayoutEligible(req.instantPayoutEligible());
        if (req.fraudScreeningLevel() != null) c.setFraudScreeningLevel(req.fraudScreeningLevel());
        if (req.chargebackAlertThreshold() != null) c.setChargebackAlertThreshold(req.chargebackAlertThreshold());
        if (req.reservePercentage() != null) c.setReservePercentage(req.reservePercentage());
        if (req.notifyOnPayout() != null) c.setNotifyOnPayout(req.notifyOnPayout());
        if (req.notifyOnChargeback() != null) c.setNotifyOnChargeback(req.notifyOnChargeback());
        if (req.notifyOnStatusChange() != null) c.setNotifyOnStatusChange(req.notifyOnStatusChange());
        if (req.kycRefreshIntervalDays() != null) c.setKycRefreshIntervalDays(req.kycRefreshIntervalDays());

        outbox.append(DomainEventType.MERCHANT_CONFIGURATION_UPDATED, MerchantService.AGGREGATE_TYPE,
                merchantId, Map.of("merchantId", merchantId.toString()));
        return toResponse(c);
    }

    private MerchantConfiguration load(UUID merchantId) {
        return repository.findByMerchantId(merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Configuration for merchant", merchantId));
    }

    private MerchantConfigurationResponse toResponse(MerchantConfiguration c) {
        return new MerchantConfigurationResponse(c.getMerchantId(), c.getCaptureMode(), c.isEnforce3ds(),
                c.getVelocityLimitPerDay(), c.getPayoutSchedule(), c.getMinimumPayoutThreshold(),
                c.isInstantPayoutEligible(), c.getFraudScreeningLevel(), c.getChargebackAlertThreshold(),
                c.getReservePercentage(), c.isNotifyOnPayout(), c.isNotifyOnChargeback(),
                c.isNotifyOnStatusChange(), c.getKycRefreshIntervalDays());
    }
}
