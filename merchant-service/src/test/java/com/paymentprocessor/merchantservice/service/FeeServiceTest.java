package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.dto.FeePreviewRequest;
import com.paymentprocessor.merchantservice.dto.FeePreviewResponse;
import com.paymentprocessor.merchantservice.entity.FeeConfiguration;
import com.paymentprocessor.merchantservice.repository.FeeConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeServiceTest {

    @Mock FeeConfigurationRepository repository;
    @Mock MerchantService merchantService;
    @InjectMocks FeeService feeService;

    @Test
    void previewComputesPercentPlusFixed() {
        UUID merchantId = UUID.randomUUID();
        FeeConfiguration fee = new FeeConfiguration();
        fee.setTransactionFeePercent(new BigDecimal("2.900"));
        fee.setTransactionFeeFixed(new BigDecimal("0.30"));
        fee.setCurrency("USD");
        when(repository.findByMerchantIdAndActiveTrue(merchantId)).thenReturn(Optional.of(fee));

        FeePreviewResponse res = feeService.preview(merchantId, new FeePreviewRequest(new BigDecimal("100.00"), "usd"));

        assertThat(res.percentComponent()).isEqualByComparingTo("2.90");
        assertThat(res.fixedComponent()).isEqualByComparingTo("0.30");
        assertThat(res.totalFee()).isEqualByComparingTo("3.20");
        assertThat(res.netToMerchant()).isEqualByComparingTo("96.80");
        assertThat(res.currency()).isEqualTo("USD");
    }
}
