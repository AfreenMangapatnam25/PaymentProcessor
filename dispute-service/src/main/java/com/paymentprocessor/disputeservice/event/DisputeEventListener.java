package com.paymentprocessor.disputeservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Stub subscriber that logs dispute domain events. It stands in for the outbound
 * bridge that would relay events to Reporting, Analytics, the Merchant Portal,
 * the Notification Service and the Audit Service.
 */
@Component
public class DisputeEventListener {

    private static final Logger log = LoggerFactory.getLogger(DisputeEventListener.class);

    @EventListener
    public void onDisputeCreated(DisputeCreatedEvent event) {
        log.info("[EVENT] DisputeCreated dispute={} merchant={} network={} amount={} {}",
                event.disputeId(), event.merchantId(), event.network(),
                event.amountMinor(), event.currency());
    }

    @EventListener
    public void onChargebackReceived(ChargebackReceivedEvent event) {
        log.info("[EVENT] ChargebackReceived dispute={} merchant={} amount={} fee={} journal={}",
                event.disputeId(), event.merchantId(), event.amountMinor(),
                event.feeMinor(), event.ledgerJournalId());
    }

    @EventListener
    public void onDisputeWon(DisputeWonEvent event) {
        log.info("[EVENT] DisputeWon dispute={} merchant={} recovered={} {} reversal={}",
                event.disputeId(), event.merchantId(), event.amountRecoveredMinor(),
                event.currency(), event.reversalJournalId());
    }

    @EventListener
    public void onDisputeLost(DisputeLostEvent event) {
        log.info("[EVENT] DisputeLost dispute={} merchant={} lost={} {} reason='{}'",
                event.disputeId(), event.merchantId(), event.amountLostMinor(),
                event.currency(), event.reason());
    }
}
