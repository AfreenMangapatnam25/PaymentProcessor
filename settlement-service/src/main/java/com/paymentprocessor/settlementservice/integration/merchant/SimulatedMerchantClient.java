package com.paymentprocessor.settlementservice.integration.merchant;

import com.paymentprocessor.settlementservice.enums.ScheduleType;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * In-process stand-in for the Merchant Service. Returns a deterministic default
 * profile for any merchant and lets tests/demos override specific merchants via
 * {@link #register(MerchantSettlementProfile)}.
 */
@Component
public class SimulatedMerchantClient implements MerchantClient {

    private final ConcurrentMap<String, MerchantSettlementProfile> profiles = new ConcurrentHashMap<>();

    /** Register or override a merchant profile (used by demos and tests). */
    public void register(MerchantSettlementProfile profile) {
        profiles.put(profile.merchantId(), profile);
    }

    @Override
    public Optional<MerchantSettlementProfile> getProfile(String merchantId) {
        return Optional.of(profiles.computeIfAbsent(merchantId, this::defaultProfile));
    }

    @Override
    public boolean isPayoutAccountValid(String merchantId, String payoutAccountId) {
        // Simulate a validation failure for accounts explicitly flagged as invalid.
        return payoutAccountId != null && !payoutAccountId.toLowerCase().contains("invalid");
    }

    private MerchantSettlementProfile defaultProfile(String merchantId) {
        return new MerchantSettlementProfile(
                merchantId,
                true,
                "USD",
                ScheduleType.DAILY,
                "acct_" + merchantId,
                290,            // 2.9% platform fee
                30,             // $0.30 fixed per transaction
                1000,           // 10% rolling reserve
                90,             // 90-day reserve hold
                true,           // pass rail fee through to merchant
                null,           // auto-select rail
                false           // no instant payout
        );
    }
}
