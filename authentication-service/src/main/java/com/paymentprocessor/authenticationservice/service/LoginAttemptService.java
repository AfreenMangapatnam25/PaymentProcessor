package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.domain.LoginResult;
import com.paymentprocessor.authenticationservice.entity.LoginAttempt;
import com.paymentprocessor.authenticationservice.repository.LoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository repository;

    public LoginAttemptService(LoginAttemptRepository repository) {
        this.repository = repository;
    }

    /** Records in its own transaction so audit trail survives even if the login rolls back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String identityId, String email, String ip, String userAgent, LoginResult result) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setIdentityId(identityId);
        attempt.setEmail(email);
        attempt.setIp(ip);
        attempt.setUserAgent(userAgent == null ? null : truncate(userAgent, 512));
        attempt.setResult(result);
        attempt.setCreatedAt(Instant.now());
        repository.save(attempt);
    }

    @Transactional(readOnly = true)
    public long countRecentFailures(String identityId, Instant since) {
        return repository.countByIdentityIdAndResultAndCreatedAtAfter(
                identityId, LoginResult.BAD_CREDENTIALS, since);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
