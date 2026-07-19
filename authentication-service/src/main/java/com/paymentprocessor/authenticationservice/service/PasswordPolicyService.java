package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.exception.BadRequestException;
import org.springframework.stereotype.Service;

/** Enforces configurable password complexity rules. */
@Service
public class PasswordPolicyService {

    private final AuthProperties.Password policy;

    public PasswordPolicyService(AuthProperties props) {
        this.policy = props.getPassword();
    }

    public void validate(String password) {
        if (password == null || password.length() < policy.getMinLength()) {
            throw new BadRequestException("Password must be at least " + policy.getMinLength() + " characters");
        }
        if (policy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            throw new BadRequestException("Password must contain an uppercase letter");
        }
        if (policy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            throw new BadRequestException("Password must contain a lowercase letter");
        }
        if (policy.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
            throw new BadRequestException("Password must contain a digit");
        }
        if (policy.isRequireSpecial() && password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new BadRequestException("Password must contain a special character");
        }
    }
}
