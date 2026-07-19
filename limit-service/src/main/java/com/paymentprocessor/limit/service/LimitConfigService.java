package com.paymentprocessor.limit.service;

import com.paymentprocessor.limit.domain.entity.LimitConfiguration;
import com.paymentprocessor.limit.domain.enums.AuditAction;
import com.paymentprocessor.limit.dto.LimitConfigRequest;
import com.paymentprocessor.limit.dto.LimitConfigResponse;
import com.paymentprocessor.limit.exception.ResourceNotFoundException;
import com.paymentprocessor.limit.repository.LimitConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CRUD management of limit configurations, with audit trailing of every change.
 */
@Service
@RequiredArgsConstructor
public class LimitConfigService {

    private final LimitConfigurationRepository repository;
    private final AuditService auditService;

    @Transactional
    public LimitConfigResponse create(LimitConfigRequest req) {
        Instant now = Instant.now();
        LimitConfiguration config = LimitConfiguration.builder()
                .name(req.name())
                .scope(req.scope())
                .scopeId(req.scopeId())
                .dimension(req.dimension())
                .timeWindow(req.timeWindow())
                .threshold(req.threshold())
                .currency(req.currency() == null ? null : req.currency().toUpperCase())
                .enforcement(req.enforcement())
                .priority(req.priority() == null ? 0 : req.priority())
                .active(req.active() == null || req.active())
                .timeZone(req.timeZone() == null ? "UTC" : req.timeZone())
                .createdAt(now)
                .updatedAt(now)
                .build();
        config = repository.save(config);
        auditService.record(AuditAction.LIMIT_CONFIG_CREATED,
                config.getId().toString(), null, "admin", LimitConfigResponse.from(config));
        return LimitConfigResponse.from(config);
    }

    @Transactional
    public LimitConfigResponse update(UUID id, LimitConfigRequest req) {
        LimitConfiguration config = getEntity(id);
        config.setName(req.name());
        config.setScope(req.scope());
        config.setScopeId(req.scopeId());
        config.setDimension(req.dimension());
        config.setTimeWindow(req.timeWindow());
        config.setThreshold(req.threshold());
        config.setCurrency(req.currency() == null ? null : req.currency().toUpperCase());
        config.setEnforcement(req.enforcement());
        if (req.priority() != null) {
            config.setPriority(req.priority());
        }
        if (req.active() != null) {
            config.setActive(req.active());
        }
        if (req.timeZone() != null) {
            config.setTimeZone(req.timeZone());
        }
        config.setUpdatedAt(Instant.now());
        config = repository.save(config);
        auditService.record(AuditAction.LIMIT_CONFIG_UPDATED,
                config.getId().toString(), null, "admin", LimitConfigResponse.from(config));
        return LimitConfigResponse.from(config);
    }

    @Transactional
    public void disable(UUID id) {
        LimitConfiguration config = getEntity(id);
        config.setActive(false);
        config.setUpdatedAt(Instant.now());
        repository.save(config);
        auditService.record(AuditAction.LIMIT_CONFIG_DISABLED,
                config.getId().toString(), null, "admin", null);
    }

    @Transactional(readOnly = true)
    public LimitConfigResponse get(UUID id) {
        return LimitConfigResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<LimitConfigResponse> listAll() {
        return repository.findAll().stream().map(LimitConfigResponse::from).toList();
    }

    private LimitConfiguration getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Limit configuration not found: " + id));
    }
}
