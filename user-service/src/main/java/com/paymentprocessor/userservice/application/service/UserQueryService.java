package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.query.GetUserQuery;
import com.paymentprocessor.userservice.domain.exception.UserNotFoundException;
import com.paymentprocessor.userservice.domain.repository.UserRepository;
import com.paymentprocessor.userservice.domain.user.User;
import com.paymentprocessor.userservice.domain.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for users. Read-only transaction so decryption happens in
 * a lightweight context and Hibernate skips dirty checking.
 */
@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(GetUserQuery query) {
        return userRepository.findById(UserId.of(query.userId()))
                .orElseThrow(() -> new UserNotFoundException(query.userId()));
    }
}
