package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.exception.InvalidStateTransitionException;
import com.paymentprocessor.settlementservice.exception.ResourceNotFoundException;
import com.paymentprocessor.settlementservice.repository.PayoutRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository-backed helper for payouts, centralising the guarded state
 * transitions used by the initiation, retry, and reversal services.
 */
@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    private final PayoutRepository repository;

    public PayoutService(PayoutRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Payout save(Payout payout) {
        return repository.save(payout);
    }

    /**
     * Transitions a payout to a new status, enforcing the payout state machine.
     */
    @Transactional
    public Payout transition(Payout payout, PayoutStatus target) {
        PayoutStatus current = payout.getStatus();
        if (current == target) {
            return payout;
        }
        if (!current.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Payout " + payout.getId(), current, target);
        }
        log.debug("Payout {} {} -> {}", payout.getId(), current, target);
        payout.setStatus(target);
        return repository.save(payout);
    }

    @Transactional(readOnly = true)
    public Payout findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payout", id));
    }

    @Transactional(readOnly = true)
    public List<Payout> findByBatch(String batchId) {
        return repository.findByBatchId(batchId);
    }

    @Transactional(readOnly = true)
    public List<Payout> findByMerchant(String merchantId) {
        return repository.findByMerchantId(merchantId);
    }

    @Transactional(readOnly = true)
    public List<Payout> findAll() {
        return repository.findAll();
    }
}
