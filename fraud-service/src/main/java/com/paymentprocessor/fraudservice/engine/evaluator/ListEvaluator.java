package com.paymentprocessor.fraudservice.engine.evaluator;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.paymentprocessor.fraudservice.domain.entity.ListEntry;
import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.engine.RiskContext;
import com.paymentprocessor.fraudservice.engine.SignalEvaluator;
import com.paymentprocessor.fraudservice.repository.ListEntryRepository;

/**
 * Highest-priority layer: blacklist and whitelist lookups.
 *
 * <p>A non-expired blacklist hit forces an immediate hard decline without score
 * evaluation. A whitelist hit dampens the final score and prevents score-only
 * auto-decline (but never overrides AML/sanctions or blacklist).
 */
@Component
public class ListEvaluator implements SignalEvaluator {

    private static final String BLACKLIST = "BLACKLIST";
    private static final String WHITELIST = "WHITELIST";

    private final ListEntryRepository lists;

    public ListEvaluator(ListEntryRepository lists) {
        this.lists = lists;
    }

    @Override
    public int order() { return 10; }

    @Override
    public String name() { return "list"; }

    @Override
    public void evaluate(FraudEvaluationRequest req, RiskContext ctx) {
        check(ctx, "CARD", req.getCardFingerprint());
        check(ctx, "CARD", req.getCardBin());
        check(ctx, "IP", req.getIpAddress());
        check(ctx, "USER", req.getUserId());
        check(ctx, "DEVICE", req.getDeviceFingerprint());
        check(ctx, "EMAIL_DOMAIN", req.getEmailDomain());
        check(ctx, "MERCHANT", req.getMerchantId());
    }

    private void check(RiskContext ctx, String attribute, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        List<ListEntry> matches = lists.findByAttributeAndValue(attribute, value);
        Instant now = Instant.now();
        for (ListEntry entry : matches) {
            if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(now)) {
                continue; // expired
            }
            if (BLACKLIST.equalsIgnoreCase(entry.getList())) {
                ctx.setHardDecline(true);
                ctx.reasonCodeIfAbsent("BLACKLIST_" + attribute);
                ctx.score("blacklist_" + attribute.toLowerCase(), "LIST", 100,
                        "Blacklisted " + attribute + ": " + reason(entry));
            } else if (WHITELIST.equalsIgnoreCase(entry.getList())) {
                ctx.setWhitelisted(true);
                ctx.score("whitelist_" + attribute.toLowerCase(), "LIST", 0,
                        "Whitelisted " + attribute + ": " + reason(entry));
            }
        }
    }

    private String reason(ListEntry entry) {
        return entry.getReason() == null ? "n/a" : entry.getReason();
    }
}
