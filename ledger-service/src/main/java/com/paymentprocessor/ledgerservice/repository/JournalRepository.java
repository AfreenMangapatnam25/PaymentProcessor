package com.paymentprocessor.ledgerservice.repository;

import com.paymentprocessor.ledgerservice.entity.Journal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalRepository extends JpaRepository<Journal, String> {

    Optional<Journal> findByIdempotencyKey(String idempotencyKey);

    List<Journal> findByExternalRef(String externalRef);

    List<Journal> findByReversesJournalId(String reversesJournalId);

    Page<Journal> findByEventType(String eventType, Pageable pageable);
}
