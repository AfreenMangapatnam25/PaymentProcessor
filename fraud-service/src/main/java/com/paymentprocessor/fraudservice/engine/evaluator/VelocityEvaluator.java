package com.paymentprocessor.fraudservice.engine.evaluator;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.engine.RiskContext;
import com.paymentprocessor.fraudservice.engine.SignalEvaluator;
import com.paymentprocessor.fraudservice.engine.velocity.VelocityStore;

/**
 * Velocity checks across the dimensions in FraudReadme.md (card / user / merchant
 * / device / IP). Records each occurrence in the {@link VelocityStore} and scores
 * threshold breaches. Also publishes counts as features for the ML layer.
 */
@Component
public class VelocityEvaluator implements SignalEvaluator {

    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration FIFTEEN_MIN = Duration.ofMinutes(15);

    private static final int CARD_PER_HOUR = 5;
    private static final int DEVICE_CARDS_PER_HOUR = 3;
    private static final int MERCHANT_PER_HOUR = 100;
    private static final int IP_PER_15MIN = 10;

    private final VelocityStore store;

    public VelocityEvaluator(VelocityStore store) {
        this.store = store;
    }

    @Override
    public int order() { return 30; }

    @Override
    public String name() { return "velocity"; }

    @Override
    public void evaluate(FraudEvaluationRequest req, RiskContext ctx) {
        if (req.getCardFingerprint() != null) {
            int n = store.hit("card:" + req.getCardFingerprint(), ONE_HOUR);
            ctx.putFeature("cardVelocity1h", n);
            if (n > CARD_PER_HOUR) {
                ctx.score("velocity_card_1h", "VELOCITY", 25,
                        n + " transactions on this card in the last hour (max " + CARD_PER_HOUR + ")");
                ctx.reasonCodeIfAbsent("VELOCITY_CARD");
            }
        }

        if (req.getDeviceFingerprint() != null && req.getCardFingerprint() != null) {
            // Distinct cards per device approximated by tagging device+card pairs.
            int n = store.hit("devcard:" + req.getDeviceFingerprint() + ":" + req.getCardFingerprint(), ONE_HOUR);
            int distinct = store.count("dev:" + req.getDeviceFingerprint(), ONE_HOUR);
            store.hit("dev:" + req.getDeviceFingerprint(), ONE_HOUR);
            ctx.putFeature("deviceCardVelocity1h", n);
            if (distinct > DEVICE_CARDS_PER_HOUR) {
                ctx.score("velocity_device_cards_1h", "VELOCITY", 22,
                        distinct + " card attempts from this device in the last hour");
                ctx.reasonCodeIfAbsent("VELOCITY_DEVICE");
            }
        }

        if (req.getMerchantId() != null) {
            int n = store.hit("merchant:" + req.getMerchantId(), ONE_HOUR);
            if (n > MERCHANT_PER_HOUR) {
                ctx.score("velocity_merchant_1h", "VELOCITY", 10,
                        n + " transactions for this merchant in the last hour");
            }
        }

        if (req.getIpAddress() != null) {
            int n = store.hit("ip:" + req.getIpAddress(), FIFTEEN_MIN);
            ctx.putFeature("ipVelocity15m", n);
            if (n > IP_PER_15MIN) {
                ctx.score("velocity_ip_15m", "VELOCITY", 20,
                        n + " attempts from this IP in 15 minutes (max " + IP_PER_15MIN + ")");
                ctx.reasonCodeIfAbsent("VELOCITY_IP");
            }
        }
    }
}
