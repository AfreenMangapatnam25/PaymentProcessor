package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {
    Optional<VerificationToken> findByTokenHash(String tokenHash);
}
