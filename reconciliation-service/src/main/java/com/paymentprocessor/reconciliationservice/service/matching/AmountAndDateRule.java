package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.MatchType;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Exact amount and transaction date within the configured business-day window. */
@Component
@Order(20)
public class AmountAndDateRule implements MatchRule {

    @Override
    public String name() {
        return "AMOUNT_AND_DATE";
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public MatchEvaluation evaluate(ReconRecord internal, ReconRecord external, ReconProperties.Matching props) {
        Integer dateVar = MatchRuleSupport.dateVarianceDays(internal, external);
        if (MatchRuleSupport.amountsEqual(internal, external)
                && dateVar != null && dateVar <= props.getDateToleranceDays()) {
            MatchType type = dateVar == 0 ? MatchType.EXACT : MatchType.TOLERANCE;
            BigDecimal confidence = dateVar == 0 ? BigDecimal.ONE : new BigDecimal("0.90");
            return new MatchEvaluation(name(), type, confidence, BigDecimal.ZERO, dateVar,
                    "Exact amount, date within " + props.getDateToleranceDays() + " day(s)");
        }
        return null;
    }
}
