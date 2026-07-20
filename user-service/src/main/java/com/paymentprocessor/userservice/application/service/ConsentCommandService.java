package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.command.GrantConsentCommand;
import com.paymentprocessor.userservice.application.command.RevokeConsentCommand;
import com.paymentprocessor.userservice.application.port.out.OutboxPort;
import com.paymentprocessor.userservice.common.id.IdGenerator;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.domain.consent.Consent;
import com.paymentprocessor.userservice.domain.consent.ConsentSubject;
import com.paymentprocessor.userservice.domain.event.ConsentGrantedEvent;
import com.paymentprocessor.userservice.domain.event.ConsentRevokedEvent;
import com.paymentprocessor.userservice.domain.exception.ConsentNotFoundException;
import com.paymentprocessor.userservice.domain.repository.ConsentRepository;
import com.paymentprocessor.userservice.domain.valueobject.ConsentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Write use-cases for consent. Grant is an upsert (create-or-update the single
 * row for the subject/kind); revoke flips an existing grant. Each operation is
 * one transaction that commits the state change and its event together.
 */
@Service
public class ConsentCommandService {

    private final ConsentRepository consentRepository;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final OutboxPort outbox;

    public ConsentCommandService(ConsentRepository consentRepository,
                                 IdGenerator idGenerator,
                                 ClockProvider clock,
                                 OutboxPort outbox) {
        this.consentRepository = consentRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.outbox = outbox;
    }

    @Transactional
    public Consent grant(GrantConsentCommand cmd) {
        ConsentSubject subject = ConsentSubject.of(cmd.subjectType(), cmd.subjectId());
        Instant now = clock.now();

        Consent consent = consentRepository.findBySubjectAndKind(subject, cmd.kind())
                .map(existing -> {
                    existing.grant(cmd.source(), cmd.policyVersion(), now);
                    return existing;
                })
                .orElseGet(() -> Consent.grantNew(
                        ConsentId.of(idGenerator.generateConsentId()),
                        subject, cmd.kind(), cmd.source(), cmd.policyVersion(), now));

        Consent saved = consentRepository.save(consent);
        outbox.append(new ConsentGrantedEvent(
                saved.getId().value(), subject.type().name(), subject.id(),
                saved.getKind().name(), saved.getPolicyVersion(), now));
        return saved;
    }

    @Transactional
    public Consent revoke(RevokeConsentCommand cmd) {
        ConsentSubject subject = ConsentSubject.of(cmd.subjectType(), cmd.subjectId());
        Consent consent = consentRepository.findBySubjectAndKind(subject, cmd.kind())
                .orElseThrow(() -> new ConsentNotFoundException(
                        subject.type().name() + ":" + subject.id() + ":" + cmd.kind()));

        Instant now = clock.now();
        consent.revoke(now);
        Consent saved = consentRepository.save(consent);
        outbox.append(new ConsentRevokedEvent(
                saved.getId().value(), subject.type().name(), subject.id(),
                saved.getKind().name(), now));
        return saved;
    }
}
