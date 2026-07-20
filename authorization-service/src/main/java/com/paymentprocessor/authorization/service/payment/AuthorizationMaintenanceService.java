package com.paymentprocessor.authorization.service.payment;

import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import com.paymentprocessor.authorization.event.AuthorizationEvent;
import com.paymentprocessor.authorization.event.EventPublisher;
import com.paymentprocessor.authorization.repository.AuthorizationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Background maintenance: expires authorization holds that lapsed without capture and purges
 * stale idempotency keys.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationMaintenanceService {

    private static final List<AuthorizationStatus> EXPIRABLE = List.of(
            AuthorizationStatus.APPROVED,
            AuthorizationStatus.PARTIALLY_APPROVED);

    private final AuthorizationRecordRepository repository;
    private final IdempotencyService idempotencyService;
    private final EventPublisher eventPublisher;

    /** Expire lapsed holds every 5 minutes. */
    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    public void expireLapsedHolds() {
        List<AuthorizationRecord> expirable = repository.findExpirable(EXPIRABLE, Instant.now());
        if (expirable.isEmpty()) {
            return;
        }
        for (AuthorizationRecord record : expirable) {
            record.setStatus(AuthorizationStatus.EXPIRED);
            repository.save(record);
            eventPublisher.publishAuthorizationEvent(
                    AuthorizationEvent.of(AuthorizationEvent.Types.EXPIRED, record));
        }
        log.info("Expired {} lapsed authorization holds", expirable.size());
    }

    /** Purge expired idempotency keys hourly. */
    @Scheduled(fixedDelayString = "PT1H")
    public void purgeIdempotencyKeys() {
        int purged = idempotencyService.purgeExpired();
        if (purged > 0) {
            log.debug("Purged {} expired idempotency keys", purged);
        }
    }
}
