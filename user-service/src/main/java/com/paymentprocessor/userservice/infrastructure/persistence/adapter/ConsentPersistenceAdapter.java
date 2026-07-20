package com.paymentprocessor.userservice.infrastructure.persistence.adapter;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.domain.consent.Consent;
import com.paymentprocessor.userservice.domain.consent.ConsentKind;
import com.paymentprocessor.userservice.domain.consent.ConsentSubject;
import com.paymentprocessor.userservice.domain.consent.SubjectType;
import com.paymentprocessor.userservice.domain.repository.ConsentRepository;
import com.paymentprocessor.userservice.domain.valueobject.ConsentId;
import com.paymentprocessor.userservice.infrastructure.persistence.entity.ConsentEntity;
import com.paymentprocessor.userservice.infrastructure.persistence.repository.ConsentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing the {@link ConsentRepository} port. Plain mapping -- no
 * encryption, since consent records hold no PII.
 */
@Component
public class ConsentPersistenceAdapter implements ConsentRepository {

    private final ConsentJpaRepository consentJpa;
    private final ClockProvider clock;

    public ConsentPersistenceAdapter(ConsentJpaRepository consentJpa, ClockProvider clock) {
        this.consentJpa = consentJpa;
        this.clock = clock;
    }

    @Override
    public Consent save(Consent consent) {
        return consentJpa.findById(consent.getId().value())
                .map(existing -> update(existing, consent))
                .orElseGet(() -> insert(consent));
    }

    private Consent insert(Consent consent) {
        ConsentEntity entity = new ConsentEntity();
        entity.setId(consent.getId().value());
        entity.setSubjectType(consent.getSubject().type().name());
        entity.setSubjectId(consent.getSubject().id());
        entity.setConsentKind(consent.getKind().name());
        entity.setCreatedAt(consent.getCreatedAt() != null ? consent.getCreatedAt() : clock.now());
        entity.setUpdatedAt(clock.now());
        applyState(entity, consent);
        ConsentEntity saved = consentJpa.save(entity);
        return consent.toBuilder().version(saved.getVersion()).build();
    }

    private Consent update(ConsentEntity entity, Consent consent) {
        if (consent.getVersion() != entity.getVersion()) {
            throw new ConflictException(ErrorCode.CONFLICT,
                    "Consent was modified concurrently; reload and retry");
        }
        applyState(entity, consent);
        ConsentEntity saved = consentJpa.saveAndFlush(entity);
        return consent.toBuilder().version(saved.getVersion()).build();
    }

    @Override
    public Optional<Consent> findBySubjectAndKind(ConsentSubject subject, ConsentKind kind) {
        return consentJpa.findBySubjectTypeAndSubjectIdAndConsentKind(
                        subject.type().name(), subject.id(), kind.name())
                .map(this::toDomain);
    }

    @Override
    public List<Consent> findBySubject(ConsentSubject subject) {
        return consentJpa.findBySubjectTypeAndSubjectIdOrderByConsentKindAsc(
                        subject.type().name(), subject.id())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private void applyState(ConsentEntity entity, Consent consent) {
        entity.setGranted(consent.isGranted());
        entity.setSource(consent.getSource());
        entity.setPolicyVersion(consent.getPolicyVersion());
        entity.setGrantedAt(consent.getGrantedAt());
        entity.setRevokedAt(consent.getRevokedAt());
    }

    private Consent toDomain(ConsentEntity entity) {
        return Consent.builder()
                .id(ConsentId.of(entity.getId()))
                .subject(ConsentSubject.of(SubjectType.valueOf(entity.getSubjectType()), entity.getSubjectId()))
                .kind(ConsentKind.valueOf(entity.getConsentKind()))
                .granted(entity.isGranted())
                .source(entity.getSource())
                .policyVersion(entity.getPolicyVersion())
                .grantedAt(entity.getGrantedAt())
                .revokedAt(entity.getRevokedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}
