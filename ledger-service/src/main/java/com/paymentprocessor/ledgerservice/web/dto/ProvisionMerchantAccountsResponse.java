package com.paymentprocessor.ledgerservice.web.dto;

public record ProvisionMerchantAccountsResponse(
        String settlementLiabilityAccountId,
        String reserveAccountId
) {
}
