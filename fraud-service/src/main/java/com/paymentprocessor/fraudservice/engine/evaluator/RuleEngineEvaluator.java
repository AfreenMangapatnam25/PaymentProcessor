package com.paymentprocessor.fraudservice.engine.evaluator;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import com.paymentprocessor.fraudservice.domain.entity.Rule;
import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.dto.RuleHit;
import com.paymentprocessor.fraudservice.engine.RiskContext;
import com.paymentprocessor.fraudservice.engine.RuleAction;
import com.paymentprocessor.fraudservice.engine.SignalEvaluator;
import com.paymentprocessor.fraudservice.repository.RuleRepository;

/**
 * Configurable rule engine. Loads enabled {@link Rule}s from MongoDB and
 * evaluates each rule's SpEL expression against the accumulated feature map.
 *
 * <p>Rules are versioned data, deployed without code changes. Expressions are
 * evaluated with a read-only, data-binding-only context ({@link SimpleEvaluationContext})
 * so authored rules cannot invoke arbitrary methods.
 *
 * <p>Example expressions:
 * <pre>
 *   amount &gt; 5000 and geoMismatch == true
 *   cardVelocity1h &gt; 3 or ipVelocity15m &gt; 8
 *   merchantChargebackRate &gt; 0.02
 * </pre>
 */
@Component
public class RuleEngineEvaluator implements SignalEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineEvaluator.class);

    /** Points assigned to a matched rule whose action is CHALLENGE (lands in the challenge band). */
    private static final int CHALLENGE_POINTS = 45;
    /** Fallback points for a SCORE rule that has no priority set. */
    private static final int DEFAULT_SCORE_POINTS = 10;

    private final RuleRepository rules;
    private final ExpressionParser parser = new SpelExpressionParser();

    public RuleEngineEvaluator(RuleRepository rules) {
        this.rules = rules;
    }

    @Override
    public int order() { return 60; }

    @Override
    public String name() { return "rules"; }

    @Override
    public void evaluate(FraudEvaluationRequest req, RiskContext ctx) {
        List<Rule> enabled = rules.findByEnabledTrue();
        enabled.sort(Comparator.comparingInt(this::priority).reversed());

        SimpleEvaluationContext spel = SimpleEvaluationContext
                .forPropertyAccessors(new MapAccessor())
                .build();

        for (Rule rule : enabled) {
            if (rule.getExpr() == null || rule.getExpr().isBlank()) {
                continue;
            }
            boolean matched;
            try {
                Expression expr = parser.parseExpression(rule.getExpr());
                Boolean value = expr.getValue(spel, ctx.getFeatures(), Boolean.class);
                matched = Boolean.TRUE.equals(value);
            } catch (Exception e) {
                log.warn("Skipping rule '{}' (id={}) — invalid expression: {}",
                        rule.getName(), rule.getId(), e.getMessage());
                continue;
            }
            if (matched) {
                apply(rule, ctx);
            }
        }
    }

    private void apply(Rule rule, RiskContext ctx) {
        RuleAction action = parseAction(rule.getAction());
        String label = rule.getName() == null ? ("rule:" + rule.getId()) : rule.getName();
        switch (action) {
            case DECLINE:
                ctx.setHardDecline(true);
                ctx.reasonCodeIfAbsent("RULE_DECLINE_" + safe(label));
                ctx.add(new RuleHit(label, "RULE", 100, action.name(), "Rule matched: decline"));
                break;
            case ESCALATE:
                ctx.setHardEscalate(true);
                ctx.reasonCodeIfAbsent("RULE_ESCALATE_" + safe(label));
                ctx.add(new RuleHit(label, "RULE", 100, action.name(), "Rule matched: escalate"));
                break;
            case REVIEW:
                ctx.setMandatoryReview(true);
                ctx.add(new RuleHit(label, "RULE", 0, action.name(), "Rule matched: route to review"));
                ctx.reasonCodeIfAbsent("RULE_REVIEW_" + safe(label));
                break;
            case CHALLENGE:
                ctx.add(new RuleHit(label, "RULE", CHALLENGE_POINTS, action.name(), "Rule matched: challenge"));
                ctx.reasonCodeIfAbsent("RULE_CHALLENGE_" + safe(label));
                break;
            case SCORE:
            default:
                int points = priority(rule) > 0 ? priority(rule) : DEFAULT_SCORE_POINTS;
                ctx.add(new RuleHit(label, "RULE", points, RuleAction.SCORE.name(), "Rule matched: +" + points));
                break;
        }
    }

    private RuleAction parseAction(String action) {
        if (action == null) {
            return RuleAction.SCORE;
        }
        try {
            return RuleAction.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RuleAction.SCORE;
        }
    }

    private int priority(Rule rule) {
        return rule.getPriority() == null ? 0 : rule.getPriority();
    }

    private String safe(String s) {
        return s.replaceAll("\\s+", "_").toUpperCase();
    }
}
