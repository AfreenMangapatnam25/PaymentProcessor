package com.paymentprocessor.fraudservice.engine.evaluator;

import org.springframework.stereotype.Component;

import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.engine.RiskContext;
import com.paymentprocessor.fraudservice.engine.SignalEvaluator;

/**
 * Merchant, user, device, and email risk signals — the "who / what device"
 * layer that complements velocity and geo checks.
 */
@Component
public class EntityRiskEvaluator implements SignalEvaluator {

    @Override
    public int order() { return 50; }

    @Override
    public String name() { return "entity"; }

    @Override
    public void evaluate(FraudEvaluationRequest req, RiskContext ctx) {
        // --- Device ---
        if (Boolean.TRUE.equals(req.getEmulator())) {
            ctx.score("device_emulator", "DEVICE", 25, "Emulator detected");
            ctx.reasonCodeIfAbsent("DEVICE_EMULATOR");
        }
        if (Boolean.TRUE.equals(req.getRooted())) {
            ctx.score("device_rooted", "DEVICE", 15, "Rooted / jailbroken device");
        }
        if (Boolean.TRUE.equals(req.getDeviceNew())) {
            ctx.score("device_new", "DEVICE", 10, "New device for this user");
            ctx.reasonCodeIfAbsent("DEVICE_NEW");
        }

        // --- Email ---
        if (Boolean.TRUE.equals(req.getDisposableEmail())) {
            ctx.score("email_disposable", "USER", 15, "Disposable email domain");
            ctx.reasonCodeIfAbsent("EMAIL_DISPOSABLE");
        }

        // --- User ---
        if (Boolean.TRUE.equals(req.getUserNewCustomer())) {
            ctx.score("user_new", "USER", 10, "First-time customer");
        }
        Integer age = req.getUserAccountAgeDays();
        if (age != null && age < 7) {
            ctx.score("user_account_new", "USER", 12, "Account younger than 7 days");
        }
        String kyc = req.getUserKycStatus();
        if (kyc != null) {
            String k = kyc.toUpperCase();
            if ("FAILED".equals(k)) {
                ctx.score("user_kyc_failed", "USER", 30, "KYC failed");
                ctx.reasonCodeIfAbsent("KYC_FAILED");
            } else if ("PENDING".equals(k) || "UNVERIFIED".equals(k)) {
                ctx.score("user_kyc_unverified", "USER", 12, "KYC " + k.toLowerCase());
            }
        }
        Integer disputes = req.getUserDisputeCount();
        if (disputes != null && disputes > 0) {
            ctx.score("user_disputes", "USER", Math.min(30, disputes * 10),
                    disputes + " prior dispute(s)/chargeback(s)");
            ctx.reasonCodeIfAbsent("USER_DISPUTE_HISTORY");
        }

        // --- Merchant ---
        Double cbr = req.getMerchantChargebackRate();
        if (cbr != null && cbr > 0.01) {
            ctx.score("merchant_chargeback_rate", "MERCHANT", 15,
                    String.format("Merchant chargeback rate %.2f%% exceeds 1%%", cbr * 100));
            ctx.reasonCodeIfAbsent("MERCHANT_HIGH_CHARGEBACK");
        }
        if (Boolean.TRUE.equals(req.getMerchantHighRisk())) {
            ctx.score("merchant_high_risk_mcc", "MERCHANT", 12, "High-risk merchant category");
        }
        Integer mAge = req.getMerchantAccountAgeDays();
        if (mAge != null && mAge < 30) {
            ctx.score("merchant_new", "MERCHANT", 10, "Newly onboarded merchant (<30 days)");
            ctx.setMandatoryReview(true); // first transactions for new merchants get reviewed
        }
    }
}
