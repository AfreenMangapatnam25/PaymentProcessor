package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.domain.CredentialKind;
import com.paymentprocessor.authenticationservice.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, String> {
    Optional<Credential> findByIdentityIdAndKindAndRotatedAtIsNull(String identityId, CredentialKind kind);
    List<Credential> findByIdentityIdAndKindOrderByCreatedAtDesc(String identityId, CredentialKind kind);
}
