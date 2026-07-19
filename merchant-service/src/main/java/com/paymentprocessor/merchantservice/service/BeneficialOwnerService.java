package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.enums.KycStatus;
import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.BeneficialOwnerRequest;
import com.paymentprocessor.merchantservice.dto.BeneficialOwnerResponse;
import com.paymentprocessor.merchantservice.entity.BeneficialOwner;
import com.paymentprocessor.merchantservice.repository.BeneficialOwnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Manages beneficial owners / directors and their KYC status. */
@Service
public class BeneficialOwnerService {

    private final BeneficialOwnerRepository repository;
    private final MerchantService merchantService;

    public BeneficialOwnerService(BeneficialOwnerRepository repository, MerchantService merchantService) {
        this.repository = repository;
        this.merchantService = merchantService;
    }

    @Transactional(readOnly = true)
    public List<BeneficialOwnerResponse> list(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return repository.findByMerchantId(merchantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public BeneficialOwnerResponse add(UUID merchantId, BeneficialOwnerRequest req) {
        merchantService.assertAccessible(merchantId);
        BeneficialOwner o = new BeneficialOwner();
        o.setMerchantId(merchantId);
        o.setKycStatus(KycStatus.PENDING);
        apply(o, req);
        return toResponse(repository.save(o));
    }

    @Transactional
    public BeneficialOwnerResponse update(UUID merchantId, UUID ownerId, BeneficialOwnerRequest req) {
        merchantService.assertAccessible(merchantId);
        BeneficialOwner o = load(merchantId, ownerId);
        apply(o, req);
        return toResponse(o);
    }

    @Transactional
    public void delete(UUID merchantId, UUID ownerId) {
        merchantService.assertAccessible(merchantId);
        repository.delete(load(merchantId, ownerId));
    }

    /** Updates KYC status, typically driven by a callback from the Compliance/KYC Service. */
    @Transactional
    public BeneficialOwnerResponse updateKycStatus(UUID merchantId, UUID ownerId, KycStatus status) {
        merchantService.assertAccessible(merchantId);
        BeneficialOwner o = load(merchantId, ownerId);
        o.setKycStatus(status);
        return toResponse(o);
    }

    private BeneficialOwner load(UUID merchantId, UUID ownerId) {
        return repository.findByIdAndMerchantId(ownerId, merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("BeneficialOwner", ownerId));
    }

    private void apply(BeneficialOwner o, BeneficialOwnerRequest req) {
        o.setFirstName(req.firstName());
        o.setLastName(req.lastName());
        o.setDateOfBirth(req.dateOfBirth());
        o.setEmail(req.email());
        o.setRole(req.role());
        o.setOwnershipPercentage(req.ownershipPercentage());
        o.setNationality(req.nationality());
    }

    private BeneficialOwnerResponse toResponse(BeneficialOwner o) {
        return new BeneficialOwnerResponse(o.getId(), o.getMerchantId(), o.getFirstName(), o.getLastName(),
                o.getDateOfBirth(), o.getEmail(), o.getRole(), o.getOwnershipPercentage(),
                o.getNationality(), o.getKycStatus());
    }
}
