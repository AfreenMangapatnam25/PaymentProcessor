package com.paymentprocessor.disputeservice.integration.stub;

import com.paymentprocessor.disputeservice.integration.SettlementClient;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development stub for {@link SettlementClient} that logs settlement / reserve
 * operations instead of performing them.
 */
@Component
public class LoggingSettlementClient implements SettlementClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingSettlementClient.class);

    @Override
    public String recoverFromSettlement(String merchantId, String disputeId,
                                        long amountMinor, String currency) {
        String ref = "stl_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[SETTLEMENT] Recover merchant={} dispute={} amount={} {} -> ref={}",
                merchantId, disputeId, amountMinor, currency, ref);
        return ref;
    }

    @Override
    public void releaseToMerchant(String merchantId, String disputeId,
                                  long amountMinor, String currency) {
        log.info("[SETTLEMENT] Release merchant={} dispute={} amount={} {}",
                merchantId, disputeId, amountMinor, currency);
    }

    @Override
    public void adjustReserve(String merchantId, int reservePercentage, int rollingDays) {
        log.info("[SETTLEMENT] Adjust reserve merchant={} pct={} rollingDays={}",
                merchantId, reservePercentage, rollingDays);
    }
}
