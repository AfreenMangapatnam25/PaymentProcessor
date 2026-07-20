package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.MatchType;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Lowest-confidence rule: exact amount and matching card last-four digits. */
@Component
@Order(50)
public class AmountAndLast4Rule implements MatchRule {

    @Override
    public String name() {
        return "AMOUNT_AND_LAST4";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public MatchEvaluation evaluate(ReconRecord internal, ReconRecord external, ReconProperties.Matching props) {
        if (MatchRuleSupport.amountsEqual(internal, external)
                && MatchRuleSupport.valuesEqual(internal.getCardLast4(), external.getCardLast4())) {
            return new MatchEvaluation(name(), MatchType.TOLERANCE, new BigDecimal("0.60"),
                    BigDecimal.ZERO, MatchRuleSupport.dateVarianceDays(internal, external),
                    "Amount and card last-4 match");
        }
        return null;
    }
}
