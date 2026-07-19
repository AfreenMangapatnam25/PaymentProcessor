package com.paymentprocessor.userservice.application.validator;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.domain.repository.UserRepository;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.IdentityId;
import org.springframework.stereotype.Component;

/**
 * Cross-aggregate uniqueness checks for users. Kept out of the domain model
 * because they require repository lookups. Error messages deliberately omit the
 * offending value so no PII leaks into logs or responses (rule 13).
 */
@Component
public class UserValidator {

    private final UserRepository userRepository;

    public UserValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateNewUser(IdentityId identityId, Email email) {
        if (userRepository.existsByIdentityId(identityId)) {
            throw new ConflictException(ErrorCode.USER_ALREADY_EXISTS,
                    "A user already exists for this identity");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(ErrorCode.USER_ALREADY_EXISTS,
                    "A user already exists with this email");
        }
    }

    public void validateEmailAvailable(Email email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(ErrorCode.USER_ALREADY_EXISTS,
                    "A user already exists with this email");
        }
    }
}
