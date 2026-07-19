package com.paymentprocessor.disputeservice.integration.stub;

import com.paymentprocessor.disputeservice.integration.PaymentClient;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development stub for {@link PaymentClient}. Logs flag changes and returns no
 * transaction detail. Replace with a real Payment Service client in production.
 */
@Component
public class LoggingPaymentClient implements PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingPaymentClient.class);

    @Override
    public void flagTransactionDisputed(String transactionId, String disputeId) {
        log.info("[PAYMENT] Flag transaction={} disputed dispute={}", transactionId, disputeId);
    }

    @Override
    public void clearDisputedFlag(String transactionId, String disputeId) {
        log.info("[PAYMENT] Clear disputed flag transaction={} dispute={}", transactionId, disputeId);
    }

    @Override
    public Optional<TransactionDetail> getTransactionDetail(String transactionId) {
        log.info("[PAYMENT] Lookup transaction={} (stub returns empty)", transactionId);
        return Optional.empty();
    }
}
