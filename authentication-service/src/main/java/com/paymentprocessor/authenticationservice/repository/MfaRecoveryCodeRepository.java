package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.entity.MfaRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, String> {
    List<MfaRecoveryCode> findByIdentityId(String identityId);
    Optional<MfaRecoveryCode> findByIdentityIdAndCodeHashAndUsedAtIsNull(String identityId, String codeHash);
    void deleteByIdentityId(String identityId);
}
