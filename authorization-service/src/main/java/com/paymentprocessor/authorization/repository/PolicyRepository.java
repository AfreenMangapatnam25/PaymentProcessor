package com.paymentprocessor.authorization.repository;

import com.paymentprocessor.authorization.domain.access.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    Optional<Policy> findByName(String name);

    @Query("""
            select p from Policy p
            where p.enabled = true and p.resource = :resource and (p.action = :action or p.action = '*')
            order by p.priority asc
            """)
    List<Policy> findApplicable(@Param("resource") String resource, @Param("action") String action);
}
