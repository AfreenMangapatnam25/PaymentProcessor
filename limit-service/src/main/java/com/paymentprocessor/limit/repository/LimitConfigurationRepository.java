package com.paymentprocessor.limit.repository;

import com.paymentprocessor.limit.domain.entity.LimitConfiguration;
import com.paymentprocessor.limit.domain.enums.EntityScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LimitConfigurationRepository extends JpaRepository<LimitConfiguration, UUID> {

    /**
     * Resolves every active configuration that could apply to a given scope value:
     * either an exact scopeId match or a scope-wide default (scopeId is null).
     */
    @Query("""
            select c from LimitConfiguration c
            where c.active = true
              and c.scope = :scope
              and (c.scopeId = :scopeId or c.scopeId is null)
            """)
    List<LimitConfiguration> findApplicable(@Param("scope") EntityScope scope,
                                            @Param("scopeId") String scopeId);

    List<LimitConfiguration> findByScopeAndScopeId(EntityScope scope, String scopeId);

    List<LimitConfiguration> findByActiveTrue();
}
