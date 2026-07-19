package com.paymentprocessor.fraudservice.engine.evaluator;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.engine.RiskContext;
import com.paymentprocessor.fraudservice.engine.SignalEvaluator;

/**
 * Anti-Money-Laundering / sanctions layer (runs right after list checks).
 *
 * <ul>
 *   <li>Sanctions or sanctioned-country hit &rarr; hard escalate (block + alert).</li>
 *   <li>PEP hit &rarr; enhanced due diligence, routed to review.</li>
 *   <li>Amount over the CTR threshold &rarr; regulatory review flag.</li>
 * </ul>
 */
@Component
public class AmlEvaluator implements SignalEvaluator {

    /** Currency Transaction Report threshold (USD-equivalent). */
    @Value("${fraud.aml.ctrThreshold:10000}")
    private BigDecimal ctrThreshold;

    @Override
    public int order() { return 20; }

    @Override
    public String name() { return "aml"; }

    @Override
    public void evaluate(FraudEvaluationRequest req, RiskContext ctx) {
        if (Boolean.TRUE.equals(req.getSanctionsHit()) || Boolean.TRUE.equals(req.getSanctionedCountry())) {
            ctx.setHardEscalate(true);
            ctx.setAmlAlert(true);
            ctx.reasonCodeIfAbsent("AML_SANCTIONS_HIT");
            ctx.score("aml_sanctions", "AML", 100, "Sanctions / OFAC match");
            return;
        }

        if (Boolean.TRUE.equals(req.getPepHit())) {
            ctx.setAmlAlert(true);
            ctx.reasonCodeIfAbsent("AML_PEP");
            ctx.score("aml_pep", "AML", 25, "Politically Exposed Person — enhanced due diligence");
        }

        if (req.getAmount() != null && req.getAmount().compareTo(ctrThreshold) >= 0) {
            ctx.setAmlAlert(true);
            ctx.reasonCodeIfAbsent("AML_CTR_THRESHOLD");
            ctx.score("aml_ctr_threshold", "AML", 10,
                    "Amount at/above CTR reporting threshold " + ctrThreshold);
        }
    }
}
