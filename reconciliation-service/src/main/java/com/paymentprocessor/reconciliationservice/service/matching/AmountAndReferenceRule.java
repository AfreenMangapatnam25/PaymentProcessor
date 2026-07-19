package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.MatchType;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Highest-confidence rule: exact amount and exact reference number. */
@Component
@Order(10)
public class AmountAndReferenceRule implements MatchRule {

    @Override
    public String name() {
        return "AMOUNT_AND_REFERENCE";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public MatchEvaluation evaluate(ReconRecord internal, ReconRecord external, ReconProperties.Matching props) {
        if (MatchRuleSupport.amountsEqual(internal, external)
                && MatchRuleSupport.referencesEqual(internal.getExternalReference(), external.getExternalReference())) {
            return new MatchEvaluation(name(), MatchType.EXACT, BigDecimal.ONE,
                    BigDecimal.ZERO, MatchRuleSupport.dateVarianceDays(internal, external),
                    "Exact amount and reference match");
        }
        return null;
    }
}
