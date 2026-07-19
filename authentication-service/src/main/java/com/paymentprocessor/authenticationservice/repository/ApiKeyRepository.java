package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import com.paymentprocessor.authenticationservice.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    Optional<ApiKey> findByPrefix(String prefix);
    List<ApiKey> findByOwnerTypeAndOwnerId(PrincipalType ownerType, String ownerId);
}
