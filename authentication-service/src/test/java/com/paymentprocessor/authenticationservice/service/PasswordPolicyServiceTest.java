package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService policy = new PasswordPolicyService(new AuthProperties());

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("Sup3rSecret!!")).doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShort() {
        assertThatThrownBy(() -> policy.validate("Ab1!"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsMissingSpecialAndDigit() {
        assertThatThrownBy(() -> policy.validate("NoDigitsHereX"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> policy.validate("alllowercase1"))
                .isInstanceOf(BadRequestException.class);
    }
}
