package com.paymentprocessor.userservice.domain.repository;

import com.paymentprocessor.userservice.domain.consent.Consent;
import com.paymentprocessor.userservice.domain.consent.ConsentKind;
import com.paymentprocessor.userservice.domain.consent.ConsentSubject;

import java.util.List;
import java.util.Optional;

/**
 * Domain port for consent persistence. Reads are subject-scoped. Speaks only in
 * domain types (rule 1).
 */
public interface ConsentRepository {

    Consent save(Consent consent);

    Optional<Consent> findBySubjectAndKind(ConsentSubject subject, ConsentKind kind);

    List<Consent> findBySubject(ConsentSubject subject);
}
