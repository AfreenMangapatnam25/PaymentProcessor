package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.query.GetConsentQuery;
import com.paymentprocessor.userservice.application.query.ListConsentsQuery;
import com.paymentprocessor.userservice.domain.consent.Consent;
import com.paymentprocessor.userservice.domain.consent.ConsentSubject;
import com.paymentprocessor.userservice.domain.exception.ConsentNotFoundException;
import com.paymentprocessor.userservice.domain.repository.ConsentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read use-cases for consent, subject-scoped.
 */
@Service
@Transactional(readOnly = true)
public class ConsentQueryService {

    private final ConsentRepository consentRepository;

    public ConsentQueryService(ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    public Consent get(GetConsentQuery query) {
        ConsentSubject subject = ConsentSubject.of(query.subjectType(), query.subjectId());
        return consentRepository.findBySubjectAndKind(subject, query.kind())
                .orElseThrow(() -> new ConsentNotFoundException(
                        subject.type().name() + ":" + subject.id() + ":" + query.kind()));
    }

    public List<Consent> list(ListConsentsQuery query) {
        return consentRepository.findBySubject(ConsentSubject.of(query.subjectType(), query.subjectId()));
    }
}
