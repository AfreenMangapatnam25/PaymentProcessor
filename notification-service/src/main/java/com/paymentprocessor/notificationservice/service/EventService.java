package com.paymentprocessor.notificationservice.service;

import com.paymentprocessor.notificationservice.entity.Event;
import com.paymentprocessor.notificationservice.repository.EventRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Read-only query layer for events. Writes go through EventIngestionService,
 * which owns idempotency and the per-merchant sequence allocation -- this
 * class intentionally doesn't expose a generic save/delete.
 */
@Service
public class EventService {

    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public Optional<Event> findById(String id) {
        return repository.findById(id);
    }

    public List<Event> findByMerchantId(String merchantId, int limit) {
        return repository.findByMerchantIdOrderBySequenceDesc(
                merchantId, PageRequest.of(0, Math.max(1, Math.min(limit, 500)), Sort.unsorted()));
    }
}
