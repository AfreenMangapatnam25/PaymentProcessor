package com.paymentprocessor.userservice.infrastructure.persistence.repository;

import com.paymentprocessor.userservice.infrastructure.persistence.entity.ConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data access to {@code consents}. Subject-scoped finders.
 */
public interface ConsentJpaRepository extends JpaRepository<ConsentEntity, String> {

    Optional<ConsentEntity> findBySubjectTypeAndSubjectIdAndConsentKind(
            String subjectType, String subjectId, String consentKind);

    List<ConsentEntity> findBySubjectTypeAndSubjectIdOrderByConsentKindAsc(
            String subjectType, String subjectId);
}
