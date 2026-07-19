package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.exception.InvalidStateTransitionException;
import com.paymentprocessor.settlementservice.exception.ResourceNotFoundException;
import com.paymentprocessor.settlementservice.repository.SettlementBatchRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository-backed helper for settlement batches, centralising guarded state
 * transitions and common queries.
 */
@Service
public class SettlementBatchService {

    private static final Logger log = LoggerFactory.getLogger(SettlementBatchService.class);

    private final SettlementBatchRepository repository;

    public SettlementBatchService(SettlementBatchRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SettlementBatch save(SettlementBatch batch) {
        return repository.save(batch);
    }

    @Transactional
    public SettlementBatch transition(SettlementBatch batch, BatchStatus target) {
        BatchStatus current = batch.getStatus();
        if (current == target) {
            return batch;
        }
        if (!current.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("SettlementBatch " + batch.getId(), current, target);
        }
        log.debug("Batch {} {} -> {}", batch.getId(), current, target);
        batch.setStatus(target);
        return repository.save(batch);
    }

    @Transactional(readOnly = true)
    public SettlementBatch findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SettlementBatch", id));
    }

    @Transactional(readOnly = true)
    public List<SettlementBatch> findByMerchant(String merchantId) {
        return repository.findByMerchantId(merchantId);
    }

    @Transactional(readOnly = true)
    public List<SettlementBatch> findByStatus(BatchStatus status) {
        return repository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<SettlementBatch> findAll() {
        return repository.findAll();
    }
}
