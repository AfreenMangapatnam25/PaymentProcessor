package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.enums.PaymentMethodType;
import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.PaymentMethodRequest;
import com.paymentprocessor.merchantservice.dto.PaymentMethodResponse;
import com.paymentprocessor.merchantservice.entity.PaymentMethodConfig;
import com.paymentprocessor.merchantservice.repository.PaymentMethodConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Controls which payment instruments a merchant may accept and their per-method settings. */
@Service
public class PaymentMethodService {

    private final PaymentMethodConfigRepository repository;
    private final MerchantService merchantService;

    public PaymentMethodService(PaymentMethodConfigRepository repository, MerchantService merchantService) {
        this.repository = repository;
        this.merchantService = merchantService;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> list(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return repository.findByMerchantId(merchantId).stream().map(this::toResponse).toList();
    }

    /** Idempotent upsert of a method configuration (enable/disable + settings). */
    @Transactional
    public PaymentMethodResponse upsert(UUID merchantId, PaymentMethodRequest req) {
        merchantService.assertAccessible(merchantId);
        PaymentMethodConfig cfg = repository.findByMerchantIdAndMethodType(merchantId, req.methodType())
                .orElseGet(() -> {
                    PaymentMethodConfig c = new PaymentMethodConfig();
                    c.setMerchantId(merchantId);
                    c.setMethodType(req.methodType());
                    return c;
                });
        cfg.setEnabled(req.enabled());
        cfg.setSettingsJson(req.settingsJson());
        return toResponse(repository.save(cfg));
    }

    @Transactional
    public PaymentMethodResponse setEnabled(UUID merchantId, PaymentMethodType methodType, boolean enabled) {
        merchantService.assertAccessible(merchantId);
        PaymentMethodConfig cfg = repository.findByMerchantIdAndMethodType(merchantId, methodType)
                .orElseThrow(() -> ResourceNotFoundException.of("PaymentMethodConfig", methodType));
        cfg.setEnabled(enabled);
        return toResponse(cfg);
    }

    private PaymentMethodResponse toResponse(PaymentMethodConfig c) {
        return new PaymentMethodResponse(c.getId(), c.getMerchantId(), c.getMethodType(),
                c.isEnabled(), c.getSettingsJson());
    }
}
