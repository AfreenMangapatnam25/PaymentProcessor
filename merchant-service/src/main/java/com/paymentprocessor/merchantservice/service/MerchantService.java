package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.enums.DomainEventType;
import com.paymentprocessor.merchantservice.common.enums.KybCaseStatus;
import com.paymentprocessor.merchantservice.common.enums.KybStatus;
import com.paymentprocessor.merchantservice.common.enums.KycStatus;
import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;
import com.paymentprocessor.merchantservice.common.enums.PricingPlan;
import com.paymentprocessor.merchantservice.common.error.BusinessRuleException;
import com.paymentprocessor.merchantservice.common.error.DuplicateResourceException;
import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.MerchantOnboardingRequest;
import com.paymentprocessor.merchantservice.dto.MerchantProfilePatchRequest;
import com.paymentprocessor.merchantservice.dto.MerchantProfileUpdateRequest;
import com.paymentprocessor.merchantservice.dto.MerchantResponse;
import com.paymentprocessor.merchantservice.dto.StatusChangeRequest;
import com.paymentprocessor.merchantservice.entity.BeneficialOwner;
import com.paymentprocessor.merchantservice.entity.FeeConfiguration;
import com.paymentprocessor.merchantservice.entity.KybCase;
import com.paymentprocessor.merchantservice.entity.Merchant;
import com.paymentprocessor.merchantservice.entity.MerchantConfiguration;
import com.paymentprocessor.merchantservice.event.OutboxWriter;
import com.paymentprocessor.merchantservice.integration.ledger.LedgerProvisioningClient;
import com.paymentprocessor.merchantservice.repository.BeneficialOwnerRepository;
import com.paymentprocessor.merchantservice.repository.FeeConfigurationRepository;
import com.paymentprocessor.merchantservice.repository.KybCaseRepository;
import com.paymentprocessor.merchantservice.repository.MerchantConfigurationRepository;
import com.paymentprocessor.merchantservice.repository.MerchantRepository;
import com.paymentprocessor.merchantservice.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the merchant aggregate: onboarding, profile management, lifecycle transitions, pricing,
 * and compliance-status propagation. All state changes emit domain events via the outbox.
 */
@Service
public class MerchantService {

    static final String AGGREGATE_TYPE = "Merchant";
    private static final String REF_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MerchantRepository merchantRepository;
    private final MerchantConfigurationRepository configurationRepository;
    private final FeeConfigurationRepository feeConfigurationRepository;
    private final KybCaseRepository kybCaseRepository;
    private final BeneficialOwnerRepository beneficialOwnerRepository;
    private final MerchantStatusPolicy statusPolicy;
    private final OutboxWriter outbox;
    private final LedgerProvisioningClient ledgerProvisioningClient;

    public MerchantService(MerchantRepository merchantRepository,
                           MerchantConfigurationRepository configurationRepository,
                           FeeConfigurationRepository feeConfigurationRepository,
                           KybCaseRepository kybCaseRepository,
                           BeneficialOwnerRepository beneficialOwnerRepository,
                           MerchantStatusPolicy statusPolicy,
                           OutboxWriter outbox,
                           LedgerProvisioningClient ledgerProvisioningClient) {
        this.merchantRepository = merchantRepository;
        this.configurationRepository = configurationRepository;
        this.feeConfigurationRepository = feeConfigurationRepository;
        this.kybCaseRepository = kybCaseRepository;
        this.beneficialOwnerRepository = beneficialOwnerRepository;
        this.statusPolicy = statusPolicy;
        this.outbox = outbox;
        this.ledgerProvisioningClient = ledgerProvisioningClient;
    }

    // ----- Onboarding -----

    @Transactional
    public MerchantResponse onboard(MerchantOnboardingRequest req) {
        if (StringUtils.hasText(req.registrationNumber())
                && merchantRepository.existsByRegistrationNumberIgnoreCase(req.registrationNumber())) {
            throw new DuplicateResourceException(
                    "A merchant with registration number " + req.registrationNumber() + " already exists");
        }

        Merchant merchant = new Merchant();
        merchant.setMerchantReference(generateReference());
        merchant.setLegalBusinessName(req.legalBusinessName());
        merchant.setTradingName(req.tradingName());
        merchant.setRegistrationNumber(req.registrationNumber());
        merchant.setTaxId(req.taxId());
        merchant.setMcc(req.mcc());
        merchant.setBusinessType(req.businessType());
        merchant.setWebsiteUrl(req.websiteUrl());
        merchant.setSupportEmail(req.supportEmail());
        merchant.setSupportPhone(req.supportPhone());
        merchant.setCountry(req.country().toUpperCase());
        merchant.setDefaultCurrency(req.defaultCurrency().toUpperCase());
        merchant.setOwnerUserId(req.ownerUserId());
        merchant.setStatus(MerchantStatus.PENDING);
        merchant.setKybStatus(KybStatus.PENDING);
        merchant.setPricingPlan(PricingPlan.STANDARD);
        merchant = merchantRepository.save(merchant);

        provisionDefaults(merchant);

        outbox.append(DomainEventType.MERCHANT_CREATED, AGGREGATE_TYPE, merchant.getId(),
                baseEventData(merchant));
        return toResponse(merchant);
    }

    private void provisionDefaults(Merchant merchant) {
        MerchantConfiguration config = new MerchantConfiguration();
        config.setMerchantId(merchant.getId());
        configurationRepository.save(config);

        FeeConfiguration fee = new FeeConfiguration();
        fee.setMerchantId(merchant.getId());
        fee.setPricingPlan(PricingPlan.STANDARD);
        fee.setTransactionFeePercent(new java.math.BigDecimal("2.900"));
        fee.setTransactionFeeFixed(new java.math.BigDecimal("0.30"));
        fee.setCurrency(merchant.getDefaultCurrency());
        fee.setActive(true);
        feeConfigurationRepository.save(fee);

        KybCase kybCase = new KybCase();
        kybCase.setMerchantId(merchant.getId());
        kybCase.setStatus(KybCaseStatus.OPEN);
        kybCase.setSubmittedAt(Instant.now());
        kybCaseRepository.save(kybCase);
    }

    // ----- Reads -----

    @Transactional(readOnly = true)
    public MerchantResponse get(UUID merchantId) {
        SecurityUtil.assertMerchantAccess(merchantId);
        return toResponse(loadMerchant(merchantId));
    }

    @Transactional(readOnly = true)
    public MerchantResponse getByReference(String merchantReference) {
        Merchant merchant = merchantRepository.findByMerchantReference(merchantReference)
                .orElseThrow(() -> ResourceNotFoundException.of("Merchant reference", merchantReference));
        return toResponse(merchant);
    }

    @Transactional(readOnly = true)
    public Page<MerchantResponse> list(MerchantStatus status, Pageable pageable) {
        if (!SecurityUtil.isAdmin()) {
            throw new AccessDeniedException("Listing all merchants requires admin privileges");
        }
        Page<Merchant> page = merchantRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    // ----- Profile -----

    @Transactional
    public MerchantResponse updateProfile(UUID merchantId, MerchantProfileUpdateRequest req) {
        SecurityUtil.assertMerchantAccess(merchantId);
        Merchant m = loadMerchant(merchantId);
        m.setLegalBusinessName(req.legalBusinessName());
        m.setTradingName(req.tradingName());
        m.setRegistrationNumber(req.registrationNumber());
        m.setTaxId(req.taxId());
        m.setMcc(req.mcc());
        m.setBusinessType(req.businessType());
        m.setWebsiteUrl(req.websiteUrl());
        m.setSupportEmail(req.supportEmail());
        m.setSupportPhone(req.supportPhone());
        outbox.append(DomainEventType.MERCHANT_UPDATED, AGGREGATE_TYPE, m.getId(), baseEventData(m));
        return toResponse(m);
    }

    @Transactional
    public MerchantResponse patchProfile(UUID merchantId, MerchantProfilePatchRequest req) {
        SecurityUtil.assertMerchantAccess(merchantId);
        Merchant m = loadMerchant(merchantId);
        if (req.legalBusinessName() != null) m.setLegalBusinessName(req.legalBusinessName());
        if (req.tradingName() != null) m.setTradingName(req.tradingName());
        if (req.registrationNumber() != null) m.setRegistrationNumber(req.registrationNumber());
        if (req.taxId() != null) m.setTaxId(req.taxId());
        if (req.mcc() != null) m.setMcc(req.mcc());
        if (req.businessType() != null) m.setBusinessType(req.businessType());
        if (req.websiteUrl() != null) m.setWebsiteUrl(req.websiteUrl());
        if (req.supportEmail() != null) m.setSupportEmail(req.supportEmail());
        if (req.supportPhone() != null) m.setSupportPhone(req.supportPhone());
        outbox.append(DomainEventType.MERCHANT_UPDATED, AGGREGATE_TYPE, m.getId(), baseEventData(m));
        return toResponse(m);
    }

    // ----- Lifecycle -----

    @Transactional
    public MerchantResponse changeStatus(UUID merchantId, StatusChangeRequest req) {
        Merchant m = loadMerchant(merchantId);
        MerchantStatus from = m.getStatus();
        MerchantStatus to = req.targetStatus();
        statusPolicy.assertTransition(from, to);

        if (to == MerchantStatus.ACTIVE) {
            assertReadyForActivation(m);
        }

        m.setStatus(to);
        Instant now = Instant.now();
        switch (to) {
            case ACTIVE -> {
                m.setActivatedAt(now);
                m.setSuspendedAt(null);
                m.setSuspensionReason(null);
            }
            case SUSPENDED -> {
                m.setSuspendedAt(now);
                m.setSuspensionReason(req.reason());
            }
            case TERMINATED -> m.setTerminatedAt(now);
            default -> { /* no extra timestamps */ }
        }

        Map<String, Object> data = baseEventData(m);
        data.put("previousStatus", from.name());
        data.put("reason", req.reason());

        if (to == MerchantStatus.ACTIVE) {
            outbox.append(DomainEventType.MERCHANT_ACTIVATED, AGGREGATE_TYPE, m.getId(), data);
            ledgerProvisioningClient.provisionMerchantAccounts(
                    m.getMerchantReference(), m.getDefaultCurrency());
        } else if (to == MerchantStatus.SUSPENDED) {
            outbox.append(DomainEventType.MERCHANT_SUSPENDED, AGGREGATE_TYPE, m.getId(), data);
        }
        outbox.append(DomainEventType.MERCHANT_STATUS_CHANGED, AGGREGATE_TYPE, m.getId(), data);
        return toResponse(m);
    }

    private void assertReadyForActivation(Merchant m) {
        if (m.getKybStatus() != KybStatus.VERIFIED) {
            throw new BusinessRuleException("Merchant cannot be activated until KYB status is VERIFIED");
        }
        List<BeneficialOwner> owners = beneficialOwnerRepository.findByMerchantId(m.getId());
        if (owners.isEmpty()) {
            throw new BusinessRuleException("Merchant cannot be activated without at least one beneficial owner");
        }
        boolean allVerified = owners.stream().allMatch(o -> o.getKycStatus() == KycStatus.VERIFIED);
        if (!allVerified) {
            throw new BusinessRuleException(
                    "Merchant cannot be activated until all beneficial owners are KYC VERIFIED");
        }
    }

    // ----- Pricing -----

    @Transactional
    public MerchantResponse assignPricingPlan(UUID merchantId, PricingPlan plan) {
        Merchant m = loadMerchant(merchantId);
        m.setPricingPlan(plan);
        Map<String, Object> data = baseEventData(m);
        data.put("pricingPlan", plan.name());
        outbox.append(DomainEventType.MERCHANT_UPDATED, AGGREGATE_TYPE, m.getId(), data);
        return toResponse(m);
    }

    // ----- Compliance propagation (called by KybService) -----

    @Transactional
    public void applyKybStatus(UUID merchantId, KybStatus kybStatus) {
        Merchant m = loadMerchant(merchantId);
        m.setKybStatus(kybStatus);
        Map<String, Object> data = baseEventData(m);
        data.put("kybStatus", kybStatus.name());
        outbox.append(DomainEventType.MERCHANT_UPDATED, AGGREGATE_TYPE, m.getId(), data);
    }

    // ----- Helpers -----

    public Merchant loadMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Merchant", merchantId));
    }

    /** Ensures the merchant exists and the caller may access it; used by child-resource services. */
    public void assertAccessible(UUID merchantId) {
        SecurityUtil.assertMerchantAccess(merchantId);
        if (!merchantRepository.existsById(merchantId)) {
            throw ResourceNotFoundException.of("Merchant", merchantId);
        }
    }

    private String generateReference() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder("mch_");
            for (int i = 0; i < 12; i++) {
                sb.append(REF_ALPHABET.charAt(RANDOM.nextInt(REF_ALPHABET.length())));
            }
            ref = sb.toString();
        } while (merchantRepository.existsByMerchantReference(ref));
        return ref;
    }

    private Map<String, Object> baseEventData(Merchant m) {
        Map<String, Object> data = new HashMap<>();
        data.put("merchantId", m.getId().toString());
        data.put("merchantReference", m.getMerchantReference());
        data.put("legalBusinessName", m.getLegalBusinessName());
        data.put("status", m.getStatus().name());
        data.put("kybStatus", m.getKybStatus().name());
        return data;
    }

    private MerchantResponse toResponse(Merchant m) {
        return new MerchantResponse(
                m.getId(), m.getMerchantReference(), m.getLegalBusinessName(), m.getTradingName(),
                m.getRegistrationNumber(), m.getTaxId(), m.getMcc(), m.getBusinessType(),
                m.getWebsiteUrl(), m.getSupportEmail(), m.getSupportPhone(), m.getCountry(),
                m.getDefaultCurrency(), m.getOwnerUserId(), m.getStatus(), m.getKybStatus(),
                m.getPricingPlan(), m.getActivatedAt(), m.getSuspendedAt(), m.getSuspensionReason(),
                m.getCreatedAt(), m.getUpdatedAt());
    }
}
