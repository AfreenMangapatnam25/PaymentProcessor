package com.paymentprocessor.settlementservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.settlementservice.entity.IdempotencyRecord;
import com.paymentprocessor.settlementservice.repository.IdempotencyRecordRepository;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Provides at-most-once semantics for mutating API calls that carry an
 * {@code Idempotency-Key}. The first call executes the action and caches its
 * serialized response; subsequent calls with the same key replay the cached
 * response without re-executing.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes {@code action} unless the key was already used, in which case the
     * cached response is returned.
     *
     * @param key          the client-supplied idempotency key (may be null/blank)
     * @param resourceType a label for the resource being created
     * @param type         the response type, for cache deserialisation
     * @param action       the operation to run on a cache miss
     */
    public <T> T execute(String key, String resourceType, Class<T> type, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        var existing = repository.findById(key);
        if (existing.isPresent()) {
            return deserialize(existing.get().getResponseBody(), type);
        }
        try {
            T result = action.get();
            persist(key, resourceType, result);
            return result;
        } catch (DataIntegrityViolationException race) {
            // A concurrent request with the same key won; replay its result.
            return repository.findById(key)
                    .map(r -> deserialize(r.getResponseBody(), type))
                    .orElseThrow(() -> race);
        }
    }

    private <T> void persist(String key, String resourceType, T result) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setResourceType(resourceType);
        record.setResponseStatus(201);
        record.setResponseBody(serialize(result));
        repository.saveAndFlush(record);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialise idempotent response", e);
        }
    }

    private <T> T deserialize(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deserialise idempotent response", e);
        }
    }
}
