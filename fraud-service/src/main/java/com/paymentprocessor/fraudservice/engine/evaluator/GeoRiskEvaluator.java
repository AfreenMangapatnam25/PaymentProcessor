package com.paymentprocessor.fraudservice.engine.evaluator;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.engine.RiskContext;
import com.paymentprocessor.fraudservice.engine.SignalEvaluator;

/**
 * External-intelligence layer: IP reputation, country risk, and BIN risk.
 * Also computes the {@code geoMismatch} feature consumed by the ML layer.
 */
@Component
public class GeoRiskEvaluator implements SignalEvaluator {

    /** Illustrative high-fraud jurisdictions (ISO-3166 alpha-2). Configurable in production. */
    private static final Set<String> HIGH_RISK_COUNTRIES =
            Set.of("NG", "GH", "PK", "ID", "VN", "UA", "RO");

    @Override
    public int order() { return 40; }

    @Override
    public String name() { return "geo"; }

    @Override
    public void evaluate(FraudEvaluationRequest req, RiskContext ctx) {
        // --- IP reputation ---
        if (Boolean.TRUE.equals(req.getIpKnownFraud())) {
            ctx.score("ip_known_fraud", "IP", 40, "IP on known-fraud / botnet feed");
            ctx.reasonCodeIfAbsent("IP_KNOWN_FRAUD");
        }
        String ipType = upper(req.getIpType());
        if ("TOR".equals(ipType)) {
            ctx.score("ip_tor", "IP", 30, "Request via TOR exit node");
            ctx.reasonCodeIfAbsent("IP_ANONYMIZED");
        } else if ("VPN".equals(ipType) || "PROXY".equals(ipType) || Boolean.TRUE.equals(req.getVpn())) {
            ctx.score("ip_anonymized", "IP", 15, "Request via VPN/proxy");
            ctx.reasonCodeIfAbsent("IP_ANONYMIZED");
        } else if ("HOSTING".equals(ipType)) {
            ctx.score("ip_hosting", "IP", 12, "Request from hosting/datacenter IP");
        }

        // --- Country risk ---
        boolean mismatch = false;
        String ipCountry = upper(req.getIpCountry());
        String billing = upper(req.getBillingCountry());
        String bin = upper(req.getBinCountry());
        if (ipCountry != null && billing != null && !ipCountry.equals(billing)) {
            mismatch = true;
        }
        if (bin != null && billing != null && !bin.equals(billing)) {
            mismatch = true;
        }
        if (mismatch) {
            ctx.score("geo_mismatch", "COUNTRY", 18,
                    "Country mismatch across IP/billing/issuing (" + ipCountry + "/" + billing + "/" + bin + ")");
            ctx.reasonCodeIfAbsent("GEO_MISMATCH");
        }
        ctx.putFeature("geoMismatch", mismatch);

        if (isHighRisk(ipCountry) || isHighRisk(billing) || isHighRisk(bin)) {
            ctx.score("high_risk_country", "COUNTRY", 15, "High-fraud jurisdiction involved");
            ctx.reasonCodeIfAbsent("HIGH_RISK_COUNTRY");
        }

        // --- Currency mismatch ---
        if (notEqualIgnoreNull(req.getCurrency(), req.getCardCurrency())) {
            ctx.score("currency_mismatch", "COUNTRY", 8,
                    "Transaction currency differs from card currency");
        }

        // --- BIN risk ---
        String cardType = upper(req.getCardType());
        if ("PREPAID".equals(cardType) || "VIRTUAL".equals(cardType)) {
            ctx.score("bin_prepaid", "BIN", 12, cardType + " card — elevated fraud rate");
            ctx.reasonCodeIfAbsent("BIN_PREPAID");
        }
    }

    private boolean isHighRisk(String country) {
        return country != null && HIGH_RISK_COUNTRIES.contains(country);
    }

    private boolean notEqualIgnoreNull(String a, String b) {
        return a != null && b != null && !a.equalsIgnoreCase(b);
    }

    private String upper(String s) {
        return s == null ? null : s.toUpperCase();
    }
}
