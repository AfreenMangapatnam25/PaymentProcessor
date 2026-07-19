package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.AddressRequest;
import com.paymentprocessor.merchantservice.dto.AddressResponse;
import com.paymentprocessor.merchantservice.entity.BusinessAddress;
import com.paymentprocessor.merchantservice.repository.BusinessAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Manages merchant business addresses, enforcing a single primary address per address type. */
@Service
public class AddressService {

    private final BusinessAddressRepository repository;
    private final MerchantService merchantService;

    public AddressService(BusinessAddressRepository repository, MerchantService merchantService) {
        this.repository = repository;
        this.merchantService = merchantService;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return repository.findByMerchantId(merchantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddressResponse add(UUID merchantId, AddressRequest req) {
        merchantService.assertAccessible(merchantId);
        BusinessAddress a = new BusinessAddress();
        a.setMerchantId(merchantId);
        apply(a, req);
        a = repository.save(a);
        if (req.primary()) {
            demoteOthers(merchantId, a);
        }
        return toResponse(a);
    }

    @Transactional
    public AddressResponse update(UUID merchantId, UUID addressId, AddressRequest req) {
        merchantService.assertAccessible(merchantId);
        BusinessAddress a = load(merchantId, addressId);
        apply(a, req);
        if (req.primary()) {
            demoteOthers(merchantId, a);
        }
        return toResponse(a);
    }

    @Transactional
    public void delete(UUID merchantId, UUID addressId) {
        merchantService.assertAccessible(merchantId);
        repository.delete(load(merchantId, addressId));
    }

    @Transactional
    public AddressResponse markPrimary(UUID merchantId, UUID addressId) {
        merchantService.assertAccessible(merchantId);
        BusinessAddress a = load(merchantId, addressId);
        a.setPrimary(true);
        demoteOthers(merchantId, a);
        return toResponse(a);
    }

    private void demoteOthers(UUID merchantId, BusinessAddress primary) {
        for (BusinessAddress other : repository.findByMerchantId(merchantId)) {
            if (!other.getId().equals(primary.getId())
                    && other.getAddressType() == primary.getAddressType()
                    && other.isPrimary()) {
                other.setPrimary(false);
            }
        }
    }

    private BusinessAddress load(UUID merchantId, UUID addressId) {
        return repository.findByIdAndMerchantId(addressId, merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Address", addressId));
    }

    private void apply(BusinessAddress a, AddressRequest req) {
        a.setAddressType(req.addressType());
        a.setLine1(req.line1());
        a.setLine2(req.line2());
        a.setCity(req.city());
        a.setRegion(req.region());
        a.setPostalCode(req.postalCode());
        a.setCountry(req.country().toUpperCase());
        a.setPrimary(req.primary());
    }

    private AddressResponse toResponse(BusinessAddress a) {
        return new AddressResponse(a.getId(), a.getMerchantId(), a.getAddressType(), a.getLine1(),
                a.getLine2(), a.getCity(), a.getRegion(), a.getPostalCode(), a.getCountry(), a.isPrimary());
    }
}
