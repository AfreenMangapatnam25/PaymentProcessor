package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.BankAccountClass;
import com.paymentprocessor.merchantservice.common.enums.BankAccountPurpose;
import jakarta.validation.constraints.*;

/** The raw account number is accepted here and encrypted at rest; it is never returned. */
public record SettlementAccountRequest(
        BankAccountPurpose purpose,
        @NotBlank @Size(max = 255) String accountHolderName,
        @Size(max = 255) String bankName,
        @Size(max = 40) String bankCode,
        @Size(max = 40) String routingNumber,
        @Size(max = 20) String swift,
        @NotBlank @Size(min = 4, max = 34) String accountNumber,
        BankAccountClass accountClass,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank @Size(min = 2, max = 2) String country,
        boolean defaultAccount
) {}
