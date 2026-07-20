package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.enums.BusinessType;
import com.paymentprocessor.merchantservice.common.enums.DomainEventType;
import com.paymentprocessor.merchantservice.common.enums.KybStatus;
import com.paymentprocessor.merchantservice.common.enums.KycStatus;
import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;
import com.paymentprocessor.merchantservice.common.error.BusinessRuleException;
import com.paymentprocessor.merchantservice.common.error.InvalidStatusTransitionException;
import com.paymentprocessor.merchantservice.dto.MerchantOnboardingRequest;
import com.paymentprocessor.merchantservice.dto.MerchantResponse;
import com.paymentprocessor.merchantservice.dto.StatusChangeRequest;
import com.paymentprocessor.merchantservice.entity.BeneficialOwner;
import com.paymentprocessor.merchantservice.entity.Merchant;
import com.paymentprocessor.merchantservice.event.OutboxWriter;
import com.paymentprocessor.merchantservice.repository.BeneficialOwnerRepository;
import com.paymentprocessor.merchantservice.repository.FeeConfigurationRepository;
import com.paymentprocessor.merchantservice.repository.KybCaseRepository;
import com.paymentprocessor.merchantservice.repository.MerchantConfigurationRepository;
import com.paymentprocessor.merchantservice.repository.MerchantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock MerchantRepository merchantRepository;
    @Mock MerchantConfigurationRepository configurationRepository;
    @Mock FeeConfigurationRepository feeConfigurationRepository;
    @Mock KybCaseRepository kybCaseRepository;
    @Mock BeneficialOwnerRepository beneficialOwnerRepository;
    @Mock OutboxWriter outbox;

    private MerchantService service() {
        return new MerchantService(merchantRepository, configurationRepository, feeConfigurationRepository,
                kybCaseRepository, beneficialOwnerRepository, new MerchantStatusPolicy(), outbox);
    }

    private MerchantOnboardingRequest onboardingRequest() {
        return new MerchantOnboardingRequest("Acme Ltd", "Acme", "REG123", "TAX123", "5411",
                BusinessType.CORPORATION, "https://acme.example", "support@acme.example", "+15550000000",
                "US", "USD", UUID.randomUUID());
    }

    @Test
    void onboardCreatesPendingMerchantAndEmitsCreatedEvent() {
        when(merchantRepository.existsByRegistrationNumberIgnoreCase("REG123")).thenReturn(false);
        when(merchantRepository.existsByMerchantReference(anyString())).thenReturn(false);
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> {
            Merchant m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        MerchantResponse res = service().onboard(onboardingRequest());

        assertThat(res.status()).isEqualTo(MerchantStatus.PENDING);
        assertThat(res.kybStatus()).isEqualTo(KybStatus.PENDING);
        assertThat(res.merchantReference()).startsWith("mch_");
        verify(outbox).append(eq(DomainEventType.MERCHANT_CREATED), eq("Merchant"), any(UUID.class), anyMap());
    }

    @Test
    void activationBlockedUntilKybVerified() {
        UUID id = UUID.randomUUID();
        Merchant m = new Merchant();
        m.setId(id);
        m.setStatus(MerchantStatus.UNDER_REVIEW);
        m.setKybStatus(KybStatus.PENDING);
        when(merchantRepository.findById(id)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service().changeStatus(id, new StatusChangeRequest(MerchantStatus.ACTIVE, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("KYB");
        verify(outbox, never()).append(eq(DomainEventType.MERCHANT_ACTIVATED), anyString(), any(), anyMap());
    }

    @Test
    void activationSucceedsWhenComplianceComplete() {
        UUID id = UUID.randomUUID();
        Merchant m = new Merchant();
        m.setId(id);
        m.setStatus(MerchantStatus.UNDER_REVIEW);
        m.setKybStatus(KybStatus.VERIFIED);
        BeneficialOwner owner = new BeneficialOwner();
        owner.setKycStatus(KycStatus.VERIFIED);
        when(merchantRepository.findById(id)).thenReturn(Optional.of(m));
        when(beneficialOwnerRepository.findByMerchantId(id)).thenReturn(List.of(owner));

        MerchantResponse res = service().changeStatus(id, new StatusChangeRequest(MerchantStatus.ACTIVE, null));

        assertThat(res.status()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(res.activatedAt()).isNotNull();
        verify(outbox).append(eq(DomainEventType.MERCHANT_ACTIVATED), eq("Merchant"), eq(id), anyMap());
        verify(outbox).append(eq(DomainEventType.MERCHANT_STATUS_CHANGED), eq("Merchant"), eq(id), anyMap());
    }

    @Test
    void illegalTransitionRejected() {
        UUID id = UUID.randomUUID();
        Merchant m = new Merchant();
        m.setId(id);
        m.setStatus(MerchantStatus.PENDING);
        when(merchantRepository.findById(id)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service().changeStatus(id, new StatusChangeRequest(MerchantStatus.ACTIVE, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
