package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.domain.MfaKind;
import com.paymentprocessor.authenticationservice.domain.MfaStatus;
import com.paymentprocessor.authenticationservice.entity.MfaFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MfaFactorRepository extends JpaRepository<MfaFactor, String> {
    List<MfaFactor> findByIdentityId(String identityId);
    List<MfaFactor> findByIdentityIdAndStatus(String identityId, MfaStatus status);
    Optional<MfaFactor> findByIdentityIdAndKindAndStatus(String identityId, MfaKind kind, MfaStatus status);
    boolean existsByIdentityIdAndStatus(String identityId, MfaStatus status);
}
