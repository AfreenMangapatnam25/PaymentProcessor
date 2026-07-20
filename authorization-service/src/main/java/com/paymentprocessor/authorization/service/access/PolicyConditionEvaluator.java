package com.paymentprocessor.authorization.service.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Evaluates a policy's JSON condition clauses against a flattened decision context (keys such as
 * {@code subject.kyc_status}, {@code resource.amount}, {@code environment.device_trust_level},
 * {@code action}). All clauses must hold (logical AND). Conditions are admin-authored data, not
 * executable code, which keeps evaluation safe and deterministic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyConditionEvaluator {

    private final ObjectMapper objectMapper;

    /** @return true when the condition JSON is empty or every clause is satisfied. */
    public boolean matches(String conditionJson, Map<String, Object> context) {
        List<PolicyCondition> clauses = parse(conditionJson);
        if (clauses.isEmpty()) {
            return true;
        }
        for (PolicyCondition clause : clauses) {
            if (!evaluate(clause, context.get(clause.attribute()))) {
                return false;
            }
        }
        return true;
    }

    private List<PolicyCondition> parse(String conditionJson) {
        if (conditionJson == null || conditionJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(conditionJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PolicyCondition.class));
        } catch (Exception e) {
            log.warn("Unparseable policy condition; treating as non-matching: {}", e.getMessage());
            return List.of(new PolicyCondition("__invalid__", PolicyCondition.Operator.EQUALS, Boolean.TRUE));
        }
    }

    private boolean evaluate(PolicyCondition clause, Object actual) {
        Object expected = clause.value();
        return switch (clause.operator()) {
            case EQUALS -> equalsLoose(actual, expected);
            case NOT_EQUALS -> !equalsLoose(actual, expected);
            case GT -> compare(actual, expected) > 0;
            case GTE -> compare(actual, expected) >= 0;
            case LT -> compare(actual, expected) < 0;
            case LTE -> compare(actual, expected) <= 0;
            case IN -> in(actual, expected);
            case CONTAINS -> contains(actual, expected);
        };
    }

    private boolean equalsLoose(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number || b instanceof Number) {
            try {
                return toBig(a).compareTo(toBig(b)) == 0;
            } catch (NumberFormatException ignored) {
                // fall through to string comparison
            }
        }
        return a.toString().equalsIgnoreCase(b.toString());
    }

    private int compare(Object a, Object b) {
        if (a == null || b == null) {
            return -1;
        }
        return toBig(a).compareTo(toBig(b));
    }

    private boolean in(Object actual, Object expected) {
        if (expected instanceof Collection<?> collection) {
            return collection.stream().anyMatch(v -> equalsLoose(actual, v));
        }
        return equalsLoose(actual, expected);
    }

    private boolean contains(Object actual, Object expected) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(v -> equalsLoose(v, expected));
        }
        return actual != null && expected != null
                && actual.toString().toLowerCase().contains(expected.toString().toLowerCase());
    }

    private BigDecimal toBig(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return new BigDecimal(value.toString());
    }
}
