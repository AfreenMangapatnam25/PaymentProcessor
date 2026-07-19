package com.paymentprocessor.disputeservice.domain.enums;

import java.util.Set;

/**
 * Lifecycle states of a dispute.
 *
 * <p>Terminal states ({@link #WON}, {@link #LOST}, {@link #ACCEPTED},
 * {@link #CLOSED}) admit no further transitions.
 */
public enum DisputeStatus {

    /** Dispute received, merchant notified, awaiting response or evidence. */
    OPEN,

    /** Merchant has been requested to provide evidence; deadline clock running. */
    PENDING_EVIDENCE,

    /** Evidence uploaded by merchant; platform reviewing for completeness. */
    EVIDENCE_REVIEW,

    /** Evidence package submitted to the card network; awaiting issuer decision. */
    REPRESENTED,

    /** Issuer rejected representment; optional pre-arbitration response required. */
    PRE_ARBITRATION,

    /** Case escalated to card network for final binding decision. */
    ARBITRATION,

    /** Merchant or platform accepted liability; no further action. */
    ACCEPTED,

    /** Dispute resolved in the merchant's favour; chargeback reversed. */
    WON,

    /** Dispute resolved against the merchant; chargeback stands. */
    LOST,

    /** Final state; no further action possible. */
    CLOSED;

    private static final Set<DisputeStatus> TERMINAL =
            Set.of(WON, LOST, ACCEPTED, CLOSED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}
