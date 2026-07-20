package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.FeeConfigurationRequest;
import com.paymentprocessor.merchantservice.dto.FeeConfigurationResponse;
import com.paymentprocessor.merchantservice.dto.FeePreviewRequest;
import com.paymentprocessor.merchantservice.dto.FeePreviewResponse;
import com.paymentprocessor.merchantservice.entity.FeeConfiguration;
import com.paymentprocessor.merchantservice.repository.FeeConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/** Manages the merchant's active fee configuration and computes fee previews. */
@Service
public class FeeService {

    private final FeeConfigurationRepository repository;
    private final MerchantService merchantService;

    public FeeService(FeeConfigurationRepository repository, MerchantService merchantService) {
        this.repository = repository;
        this.merchantService = merchantService;
    }

    @Transactional(readOnly = true)
    public FeeConfigurationResponse getActive(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return toResponse(loadActive(merchantId));
    }

    @Transactional
    public FeeConfigurationResponse configure(UUID merchantId, FeeConfigurationRequest req) {
        merchantService.assertAccessible(merchantId);
        // Deactivate the current active configuration (retained for audit) and create a new one.
        repository.findByMerchantIdAndActiveTrue(merchantId).ifPresent(current -> current.setActive(false));
        FeeConfiguration fee = new FeeConfiguration();
        fee.setMerchantId(merchantId);
        fee.setPricingPlan(req.pricingPlan());
        fee.setTransactionFeePercent(req.transactionFeePercent());
        fee.setTransactionFeeFixed(req.transactionFeeFixed());
        fee.setInterchangePassThrough(req.interchangePassThrough());
        fee.setMonthlyPlatformFee(req.monthlyPlatformFee());
        fee.setChargebackFee(req.chargebackFee());
        fee.setRefundFee(req.refundFee());
        fee.setPayoutFee(req.payoutFee());
        fee.setCurrency(req.currency().toUpperCase());
        fee.setActive(true);
        return toResponse(repository.save(fee));
    }

    @Transactional(readOnly = true)
    public FeePreviewResponse preview(UUID merchantId, FeePreviewRequest req) {
        merchantService.assertAccessible(merchantId);
        FeeConfiguration fee = loadActive(merchantId);
        BigDecimal percentRate = nz(fee.getTransactionFeePercent());
        BigDecimal fixed = nz(fee.getTransactionFeeFixed());
        BigDecimal percentComponent = req.amount()
                .multiply(percentRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = percentComponent.add(fixed).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = req.amount().subtract(total).setScale(2, RoundingMode.HALF_UP);
        return new FeePreviewResponse(req.amount(), req.currency().toUpperCase(), percentComponent, fixed, total, net);
    }

    private FeeConfiguration loadActive(UUID merchantId) {
        return repository.findByMerchantIdAndActiveTrue(merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Active fee configuration for merchant", merchantId));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private FeeConfigurationResponse toResponse(FeeConfiguration f) {
        return new FeeConfigurationResponse(f.getId(), f.getMerchantId(), f.getPricingPlan(),
                f.getTransactionFeePercent(), f.getTransactionFeeFixed(), f.isInterchangePassThrough(),
                f.getMonthlyPlatformFee(), f.getChargebackFee(), f.getRefundFee(), f.getPayoutFee(),
                f.getCurrency(), f.isActive());
    }
}
