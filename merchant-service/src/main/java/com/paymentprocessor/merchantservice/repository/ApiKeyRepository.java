package com.paymentprocessor.merchantservice.repository;

import com.paymentprocessor.merchantservice.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyId(String keyId);
    List<ApiKey> findByMerchantId(UUID merchantId);
    Optional<ApiKey> findByIdAndMerchantId(UUID id, UUID merchantId);

    /** Lightweight last-used touch that does not bump the optimistic-lock version. */
    @Modifying
    @Query("update ApiKey k set k.lastUsedAt = :ts where k.keyId = :keyId")
    int touchLastUsed(@Param("keyId") String keyId, @Param("ts") Instant ts);
}
