package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;
import com.paymentprocessor.merchantservice.common.error.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantStatusPolicyTest {

    private final MerchantStatusPolicy policy = new MerchantStatusPolicy();

    @Test
    void allowsHappyPathOnboarding() {
        assertThat(policy.canTransition(MerchantStatus.PENDING, MerchantStatus.UNDER_REVIEW)).isTrue();
        assertThat(policy.canTransition(MerchantStatus.UNDER_REVIEW, MerchantStatus.ACTIVE)).isTrue();
        assertThat(policy.canTransition(MerchantStatus.ACTIVE, MerchantStatus.SUSPENDED)).isTrue();
        assertThat(policy.canTransition(MerchantStatus.SUSPENDED, MerchantStatus.ACTIVE)).isTrue();
    }

    @Test
    void rejectsIllegalAndTerminalTransitions() {
        assertThat(policy.canTransition(MerchantStatus.PENDING, MerchantStatus.ACTIVE)).isFalse();
        assertThat(policy.canTransition(MerchantStatus.TERMINATED, MerchantStatus.ACTIVE)).isFalse();
        assertThat(policy.canTransition(MerchantStatus.BLACKLISTED, MerchantStatus.ACTIVE)).isFalse();
        assertThat(policy.canTransition(MerchantStatus.ACTIVE, MerchantStatus.ACTIVE)).isFalse();
    }

    @Test
    void assertTransitionThrowsOnIllegal() {
        assertThatThrownBy(() -> policy.assertTransition(MerchantStatus.PENDING, MerchantStatus.ACTIVE))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
