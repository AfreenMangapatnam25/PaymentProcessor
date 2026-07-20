package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.MatchType;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Same reference with the amount inside the absolute or FX-percentage tolerance band. Captures FX
 * rounding differences that should still be treated as a match, recording the residual variance.
 */
@Component
@Order(40)
public class ReferenceWithToleranceRule implements MatchRule {

    @Override
    public String name() {
        return "REFERENCE_AMOUNT_TOLERANCE";
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public MatchEvaluation evaluate(ReconRecord internal, ReconRecord external, ReconProperties.Matching props) {
        if (!MatchRuleSupport.referencesEqual(internal.getExternalReference(), external.getExternalReference())) {
            return null;
        }
        if (MatchRuleSupport.amountsEqual(internal, external)) {
            return null; // handled by the higher-priority exact rule
        }
        boolean withinAbsolute = MatchRuleSupport.amountsWithinAbsolute(internal, external, props.getAmountTolerance());
        boolean withinPercent = MatchRuleSupport.amountsWithinPercent(internal, external, props.getAmountTolerancePercent());
        if (withinAbsolute || withinPercent) {
            BigDecimal variance = MatchRuleSupport.amountVariance(internal, external);
            BigDecimal confidence = withinAbsolute ? new BigDecimal("0.92") : new BigDecimal("0.80");
            return new MatchEvaluation(name(), MatchType.TOLERANCE, confidence, variance,
                    MatchRuleSupport.dateVarianceDays(internal, external),
                    "Reference match with amount variance " + variance);
        }
        return null;
    }
}
