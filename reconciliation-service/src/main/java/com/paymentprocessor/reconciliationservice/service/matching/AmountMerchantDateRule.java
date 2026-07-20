package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.MatchType;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Exact amount, same merchant and same date. */
@Component
@Order(30)
public class AmountMerchantDateRule implements MatchRule {

    @Override
    public String name() {
        return "AMOUNT_MERCHANT_DATE";
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public MatchEvaluation evaluate(ReconRecord internal, ReconRecord external, ReconProperties.Matching props) {
        Integer dateVar = MatchRuleSupport.dateVarianceDays(internal, external);
        if (MatchRuleSupport.amountsEqual(internal, external)
                && MatchRuleSupport.valuesEqual(internal.getMerchantId(), external.getMerchantId())
                && dateVar != null && dateVar == 0) {
            return new MatchEvaluation(name(), MatchType.EXACT, new BigDecimal("0.85"),
                    BigDecimal.ZERO, 0, "Exact amount, merchant and date match");
        }
        return null;
    }
}
