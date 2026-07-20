package com.paymentprocessor.limit.service;

import com.paymentprocessor.limit.domain.entity.LimitConfiguration;
import com.paymentprocessor.limit.domain.enums.EntityScope;
import com.paymentprocessor.limit.repository.LimitConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the set of limit configurations that apply to a transaction across all
 * scopes (global, merchant, customer, currency, country).
 *
 * <p>Override semantics: within a group of the same (scope, dimension, timeWindow,
 * currency), a configuration bound to a specific entity id overrides the
 * scope-wide default (scopeId = null). This lets an individual customer or merchant
 * be granted a higher (or lower) limit than the platform default without the
 * default also firing.
 */
@Service
@RequiredArgsConstructor
public class LimitResolver {

    private final LimitConfigurationRepository configRepository;

    public List<LimitConfiguration> resolve(String customerId, String merchantId,
                                            String currency, String country) {
        List<LimitConfiguration> candidates = new ArrayList<>();
        candidates.addAll(configRepository.findApplicable(EntityScope.GLOBAL, null));
        if (StringUtils.hasText(customerId)) {
            candidates.addAll(configRepository.findApplicable(EntityScope.CUSTOMER, customerId));
        }
        if (StringUtils.hasText(merchantId)) {
            candidates.addAll(configRepository.findApplicable(EntityScope.MERCHANT, merchantId));
        }
        if (StringUtils.hasText(currency)) {
            candidates.addAll(configRepository.findApplicable(EntityScope.CURRENCY, currency));
        }
        if (StringUtils.hasText(country)) {
            candidates.addAll(configRepository.findApplicable(EntityScope.COUNTRY, country));
        }

        // Keep only limits whose currency matches the transaction (or is unset).
        List<LimitConfiguration> matching = candidates.stream()
                .filter(c -> c.getCurrency() == null || c.getCurrency().equalsIgnoreCase(currency))
                .toList();

        return applyOverrides(matching);
    }

    /**
     * Within each (scope, dimension, timeWindow, currency) group, prefer a
     * specific-entity configuration over the scope-wide default.
     */
    private List<LimitConfiguration> applyOverrides(List<LimitConfiguration> configs) {
        Map<String, LimitConfiguration> chosen = new LinkedHashMap<>();
        for (LimitConfiguration c : configs) {
            String key = c.getScope() + "|" + c.getDimension() + "|" + c.getTimeWindow()
                    + "|" + (c.getCurrency() == null ? "*" : c.getCurrency().toUpperCase());
            LimitConfiguration existing = chosen.get(key);
            if (existing == null) {
                chosen.put(key, c);
            } else if (existing.getScopeId() == null && c.getScopeId() != null) {
                // Specific entity overrides the scope-wide default.
                chosen.put(key, c);
            }
        }
        return chosen.values().stream()
                .sorted(Comparator.comparingInt(LimitConfiguration::getPriority).reversed()
                        .thenComparing(c -> c.getId().toString()))
                .toList();
    }
}
