package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.entity.SettlementItem;
import com.paymentprocessor.settlementservice.exception.ResourceNotFoundException;
import com.paymentprocessor.settlementservice.repository.SettlementItemRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests and queries the settlement line items that feed the calculation
 * engine. Ingestion is idempotent on the item's {@code idempotencyKey}, so the
 * same upstream event (capture, refund, chargeback, fee) can be replayed safely.
 */
@Service
public class SettlementItemService {

    private static final Logger log = LoggerFactory.getLogger(SettlementItemService.class);

    private final SettlementItemRepository repository;

    public SettlementItemService(SettlementItemRepository repository) {
        this.repository = repository;
    }

    /**
     * Ingests a new settlement item unless one with the same idempotency key was
     * already ingested, in which case the existing item is returned unchanged.
     */
    @Transactional
    public SettlementItem ingest(SettlementItem item) {
        if (item.getEffectiveAt() == null) {
            item.setEffectiveAt(Instant.now());
        }
        if (item.getIdempotencyKey() != null && repository.existsByIdempotencyKey(item.getIdempotencyKey())) {
            log.debug("Skipping duplicate settlement item key={}", item.getIdempotencyKey());
            return item;
        }
        return repository.save(item);
    }

    @Transactional(readOnly = true)
    public List<SettlementItem> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public SettlementItem findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SettlementItem", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public List<SettlementItem> findByBatch(String batchId) {
        return repository.findByBatchId(batchId);
    }
}
