package com.paymentprocessor.merchantservice.dto;

import java.math.BigDecimal;

public record FeePreviewResponse(
        BigDecimal amount, String currency, BigDecimal percentComponent,
        BigDecimal fixedComponent, BigDecimal totalFee, BigDecimal netToMerchant
) {}
