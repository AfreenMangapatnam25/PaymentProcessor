package com.paymentprocessor.authorization.service.access;

import com.paymentprocessor.authorization.domain.access.Policy;
import com.paymentprocessor.authorization.domain.enums.PolicyEffect;
import com.paymentprocessor.authorization.domain.enums.PolicyType;
import com.paymentprocessor.authorization.repository.PolicyRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ABAC / conditional policy engine. Applicable policies for a {@code resource:action} are evaluated
 * in priority order against the decision context. Conflict resolution is deny-by-default: an
 * explicit matching DENY always overrides any ALLOW.
 */
@Service
@RequiredArgsConstructor
public class PolicyEvaluationService {

    private final PolicyRepository policyRepository;
    private final PolicyConditionEvaluator conditionEvaluator;

    @Builder
    public record PolicyEvaluationResult(
            boolean explicitDeny,
            String denyPolicy,
            boolean explicitAllow,
            boolean stepUpRequired,
            List<String> evaluatedPolicies
    ) {
    }

    @Transactional(readOnly = true)
    public PolicyEvaluationResult evaluate(String resource, String action, Map<String, Object> context) {
        List<Policy> policies = policyRepository.findApplicable(resource, action);
        List<String> evaluated = new ArrayList<>();
        boolean explicitAllow = false;
        boolean stepUpRequired = false;

        for (Policy policy : policies) {
            evaluated.add(policy.getName());
            boolean matched = conditionEvaluator.matches(policy.getConditionJson(), context);
            if (matched && policy.getEffect() == PolicyEffect.DENY) {
                return PolicyEvaluationResult.builder()
                        .explicitDeny(true)
                        .denyPolicy(policy.getName())
                        .evaluatedPolicies(evaluated)
                        .build();
            }
            if (matched && policy.getEffect() == PolicyEffect.ALLOW) {
                explicitAllow = true;
            }
            if (!matched && policy.getType() == PolicyType.CONDITIONAL
                    && policy.getEffect() == PolicyEffect.ALLOW) {
                stepUpRequired = true;
            }
        }

        return PolicyEvaluationResult.builder()
                .explicitAllow(explicitAllow)
                .stepUpRequired(stepUpRequired)
                .evaluatedPolicies(evaluated)
                .build();
    }
}
