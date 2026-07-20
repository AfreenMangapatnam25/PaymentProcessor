package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.AccountVerificationStatus;
import com.paymentprocessor.merchantservice.common.enums.BankAccountClass;
import com.paymentprocessor.merchantservice.common.enums.BankAccountPurpose;
import java.util.UUID;

/** Note: only the last 4 digits of the account number are exposed. */
public record SettlementAccountResponse(
        UUID id, UUID merchantId, BankAccountPurpose purpose, String accountHolderName,
        String bankName, String bankCode, String routingNumber, String swift,
        String accountNumberLast4, BankAccountClass accountClass, String currency,
        String country, boolean defaultAccount, AccountVerificationStatus verificationStatus
) {}
