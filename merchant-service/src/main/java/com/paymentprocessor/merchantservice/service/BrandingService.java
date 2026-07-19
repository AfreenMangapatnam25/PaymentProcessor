package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.dto.BrandingRequest;
import com.paymentprocessor.merchantservice.dto.BrandingResponse;
import com.paymentprocessor.merchantservice.entity.MerchantBranding;
import com.paymentprocessor.merchantservice.repository.MerchantBrandingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Manages merchant checkout/receipt branding (created on first update). */
@Service
public class BrandingService {

    private final MerchantBrandingRepository repository;
    private final MerchantService merchantService;

    public BrandingService(MerchantBrandingRepository repository, MerchantService merchantService) {
        this.repository = repository;
        this.merchantService = merchantService;
    }

    @Transactional(readOnly = true)
    public BrandingResponse get(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return repository.findByMerchantId(merchantId)
                .map(this::toResponse)
                .orElse(new BrandingResponse(merchantId, null, null, null, null, null, null));
    }

    @Transactional
    public BrandingResponse update(UUID merchantId, BrandingRequest req) {
        merchantService.assertAccessible(merchantId);
        MerchantBranding b = repository.findByMerchantId(merchantId).orElseGet(() -> {
            MerchantBranding nb = new MerchantBranding();
            nb.setMerchantId(merchantId);
            return nb;
        });
        b.setLogoUrl(req.logoUrl());
        b.setPrimaryColor(req.primaryColor());
        b.setSecondaryColor(req.secondaryColor());
        b.setStatementDescriptor(req.statementDescriptor());
        b.setCustomDomain(req.customDomain());
        b.setEmailTemplateRef(req.emailTemplateRef());
        return toResponse(repository.save(b));
    }

    private BrandingResponse toResponse(MerchantBranding b) {
        return new BrandingResponse(b.getMerchantId(), b.getLogoUrl(), b.getPrimaryColor(),
                b.getSecondaryColor(), b.getStatementDescriptor(), b.getCustomDomain(), b.getEmailTemplateRef());
    }
}
