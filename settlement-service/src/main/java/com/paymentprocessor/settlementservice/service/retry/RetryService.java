package com.paymentprocessor.settlementservice.service.retry;

import com.paymentprocessor.settlementservice.config.SettlementProperties;
import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.enums.FailureCategory;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.exception.RailException;
import com.paymentprocessor.settlementservice.integration.events.DomainEventPublisher;
import com.paymentprocessor.settlementservice.integration.events.EventTypes;
import com.paymentprocessor.settlementservice.repository.PayoutRepository;
import com.paymentprocessor.settlementservice.service.PayoutService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classifies payout failures and drives the retry lifecycle: retryable failures
 * are rescheduled with exponential backoff; non-retryable or exhausted failures
 * are terminated and a {@code SettlementFailed} event is published.
 */
@Service
public class RetryService {

    private static final Logger log = LoggerFactory.getLogger(RetryService.class);

    private final SettlementProperties properties;
    private final PayoutService payoutService;
    private final PayoutRepository payoutRepository;
    private final DomainEventPublisher eventPublisher;

    public RetryService(SettlementProperties properties,
                        PayoutService payoutService,
                        PayoutRepository payoutRepository,
                        DomainEventPublisher eventPublisher) {
        this.properties = properties;
        this.payoutService = payoutService;
        this.payoutRepository = payoutRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Applies the failure policy to a payout that a rail rejected.
     *
     * @return the updated payout (RETRY_SCHEDULED or FAILED)
     */
    @Transactional
    public Payout handleFailure(Payout payout, RailException ex) {
        payout.setFailureCode(ex.getFailureCode());
        payout.setFailureReason(ex.getMessage());
        payout.setFailureCategory(ex.getCategory());

        FailureCategory category = ex.getCategory();
        int attempts = payout.getAttemptCount();
        int maxAttempts = properties.getRetry().getMaxAttempts();

        if (category.isRetryable() && attempts < maxAttempts) {
            int backoffMinutes = properties.getRetry().backoffForAttempt(attempts);
            payout.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(backoffMinutes)));
            Payout scheduled = payoutService.transition(payout, PayoutStatus.RETRY_SCHEDULED);
            if (attempts >= 2) {
                log.warn("ALERT: payout {} failed {} times (category={}, code={}); next retry in {}m",
                        payout.getId(), attempts, category, ex.getFailureCode(), backoffMinutes);
            } else {
                log.info("Payout {} scheduled for retry #{} in {}m (category={})",
                        payout.getId(), attempts + 1, backoffMinutes, category);
            }
            return scheduled;
        }

        payout.setNextRetryAt(null);
        Payout failed = payoutService.transition(payout, PayoutStatus.FAILED);
        if (category == FailureCategory.FATAL) {
            log.error("ESCALATE to compliance: payout {} fatally failed (code={}): {}",
                    payout.getId(), ex.getFailureCode(), ex.getMessage());
        } else {
            log.error("Payout {} permanently failed after {} attempts (category={}, code={})",
                    payout.getId(), attempts, category, ex.getFailureCode());
        }
        eventPublisher.publish(EventTypes.SETTLEMENT_FAILED, "Payout", payout.getId(),
                Map.of("payoutId", payout.getId(),
                        "merchantId", payout.getMerchantId(),
                        "batchId", payout.getBatchId(),
                        "amountMinor", payout.getAmountMinor(),
                        "currency", payout.getCurrency(),
                        "failureCode", ex.getFailureCode(),
                        "failureCategory", category.name()));
        return failed;
    }

    /** Payouts whose scheduled retry time has arrived. */
    @Transactional(readOnly = true)
    public List<Payout> dueRetries(Instant now) {
        return payoutRepository.findByStatusAndNextRetryAtLessThanEqual(PayoutStatus.RETRY_SCHEDULED, now);
    }
}
