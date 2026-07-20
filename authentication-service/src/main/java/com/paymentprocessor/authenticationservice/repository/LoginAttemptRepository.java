package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.domain.LoginResult;
import com.paymentprocessor.authenticationservice.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    long countByIdentityIdAndResultAndCreatedAtAfter(String identityId, LoginResult result, Instant after);
    long countByIpAndCreatedAtAfter(String ip, Instant after);
}
