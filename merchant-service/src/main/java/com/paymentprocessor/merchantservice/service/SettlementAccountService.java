package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.crypto.EncryptionService;
import com.paymentprocessor.merchantservice.common.enums.AccountVerificationStatus;
import com.paymentprocessor.merchantservice.common.enums.BankAccountPurpose;
import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.SettlementAccountRequest;
import com.paymentprocessor.merchantservice.dto.SettlementAccountResponse;
import com.paymentprocessor.merchantservice.entity.SettlementAccount;
import com.paymentprocessor.merchantservice.repository.SettlementAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages settlement/bank accounts. Full account numbers are encrypted at rest and never
 * returned; only the last four digits are exposed.
 */
@Service
public class SettlementAccountService {

    private final SettlementAccountRepository repository;
    private final MerchantService merchantService;
    private final EncryptionService encryptionService;

    public SettlementAccountService(SettlementAccountRepository repository, MerchantService merchantService,
                                    EncryptionService encryptionService) {
        this.repository = repository;
        this.merchantService = merchantService;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public List<SettlementAccountResponse> list(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return repository.findByMerchantId(merchantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SettlementAccountResponse add(UUID merchantId, SettlementAccountRequest req) {
        merchantService.assertAccessible(merchantId);
        SettlementAccount acc = new SettlementAccount();
        acc.setMerchantId(merchantId);
        applyImmutableNumber(acc, req.accountNumber());
        applyEditable(acc, req);
        acc = repository.save(acc);
        if (req.defaultAccount()) {
            demoteOthers(merchantId, acc);
        }
        return toResponse(acc);
    }

    @Transactional
    public SettlementAccountResponse update(UUID merchantId, UUID accountId, SettlementAccountRequest req) {
        merchantService.assertAccessible(merchantId);
        SettlementAccount acc = load(merchantId, accountId);
        // Changing the account number re-encrypts and resets verification.
        applyImmutableNumber(acc, req.accountNumber());
        acc.setVerificationStatus(AccountVerificationStatus.UNVERIFIED);
        applyEditable(acc, req);
        if (req.defaultAccount()) {
            demoteOthers(merchantId, acc);
        }
        return toResponse(acc);
    }

    @Transactional
    public void delete(UUID merchantId, UUID accountId) {
        merchantService.assertAccessible(merchantId);
        repository.delete(load(merchantId, accountId));
    }

    @Transactional
    public SettlementAccountResponse markDefault(UUID merchantId, UUID accountId) {
        merchantService.assertAccessible(merchantId);
        SettlementAccount acc = load(merchantId, accountId);
        acc.setDefaultAccount(true);
        demoteOthers(merchantId, acc);
        return toResponse(acc);
    }

    /** Simulates completion of account verification (micro-deposit / instant). */
    @Transactional
    public SettlementAccountResponse markVerified(UUID merchantId, UUID accountId) {
        merchantService.assertAccessible(merchantId);
        SettlementAccount acc = load(merchantId, accountId);
        acc.setVerificationStatus(AccountVerificationStatus.VERIFIED);
        return toResponse(acc);
    }

    private void demoteOthers(UUID merchantId, SettlementAccount def) {
        for (SettlementAccount other : repository.findByMerchantId(merchantId)) {
            if (!other.getId().equals(def.getId())
                    && other.getPurpose() == def.getPurpose()
                    && other.isDefaultAccount()) {
                other.setDefaultAccount(false);
            }
        }
    }

    private void applyImmutableNumber(SettlementAccount acc, String accountNumber) {
        acc.setAccountNumberEncrypted(encryptionService.encrypt(accountNumber));
        acc.setAccountNumberLast4(last4(accountNumber));
    }

    private void applyEditable(SettlementAccount acc, SettlementAccountRequest req) {
        acc.setPurpose(req.purpose() != null ? req.purpose() : BankAccountPurpose.SETTLEMENT);
        acc.setAccountHolderName(req.accountHolderName());
        acc.setBankName(req.bankName());
        acc.setBankCode(req.bankCode());
        acc.setRoutingNumber(req.routingNumber());
        acc.setSwift(req.swift());
        acc.setAccountClass(req.accountClass());
        acc.setCurrency(req.currency().toUpperCase());
        acc.setCountry(req.country().toUpperCase());
        acc.setDefaultAccount(req.defaultAccount());
    }

    private String last4(String number) {
        String digits = number.replaceAll("\\s", "");
        return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    }

    private SettlementAccount load(UUID merchantId, UUID accountId) {
        return repository.findByIdAndMerchantId(accountId, merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("SettlementAccount", accountId));
    }

    private SettlementAccountResponse toResponse(SettlementAccount a) {
        return new SettlementAccountResponse(a.getId(), a.getMerchantId(), a.getPurpose(),
                a.getAccountHolderName(), a.getBankName(), a.getBankCode(), a.getRoutingNumber(),
                a.getSwift(), a.getAccountNumberLast4(), a.getAccountClass(), a.getCurrency(),
                a.getCountry(), a.isDefaultAccount(), a.getVerificationStatus());
    }
}
