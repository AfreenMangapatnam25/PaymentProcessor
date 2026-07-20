package com.paymentprocessor.disputeservice.domain;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.exception.InvalidDisputeStateException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the legal transitions of the dispute lifecycle. All status
 * changes in the service flow through {@link #assertCanTransition} so that the
 * state machine is the single source of truth for what is permitted.
 *
 * <pre>
 *   OPEN ─────────────► PENDING_EVIDENCE ─► EVIDENCE_REVIEW ─► REPRESENTED
 *     │                        │                   │                │
 *     └─► ACCEPTED             └─► ACCEPTED         └─► ACCEPTED     ├─► WON
 *                                                                   ├─► LOST
 *                                                                   └─► PRE_ARBITRATION
 *   PRE_ARBITRATION ─► ARBITRATION ─► WON | LOST
 *   WON | LOST | ACCEPTED ─► CLOSED
 * </pre>
 */
public final class DisputeStateMachine {

    private static final Map<DisputeStatus, Set<DisputeStatus>> TRANSITIONS =
            new EnumMap<>(DisputeStatus.class);

    static {
        TRANSITIONS.put(DisputeStatus.OPEN,
                EnumSet.of(DisputeStatus.PENDING_EVIDENCE, DisputeStatus.ACCEPTED,
                        DisputeStatus.LOST));
        TRANSITIONS.put(DisputeStatus.PENDING_EVIDENCE,
                EnumSet.of(DisputeStatus.EVIDENCE_REVIEW, DisputeStatus.ACCEPTED,
                        DisputeStatus.LOST));
        TRANSITIONS.put(DisputeStatus.EVIDENCE_REVIEW,
                EnumSet.of(DisputeStatus.REPRESENTED, DisputeStatus.ACCEPTED,
                        DisputeStatus.LOST));
        TRANSITIONS.put(DisputeStatus.REPRESENTED,
                EnumSet.of(DisputeStatus.WON, DisputeStatus.LOST,
                        DisputeStatus.PRE_ARBITRATION));
        TRANSITIONS.put(DisputeStatus.PRE_ARBITRATION,
                EnumSet.of(DisputeStatus.ARBITRATION, DisputeStatus.ACCEPTED,
                        DisputeStatus.LOST, DisputeStatus.WON));
        TRANSITIONS.put(DisputeStatus.ARBITRATION,
                EnumSet.of(DisputeStatus.WON, DisputeStatus.LOST));
        TRANSITIONS.put(DisputeStatus.ACCEPTED, EnumSet.of(DisputeStatus.CLOSED));
        TRANSITIONS.put(DisputeStatus.WON, EnumSet.of(DisputeStatus.CLOSED));
        TRANSITIONS.put(DisputeStatus.LOST, EnumSet.of(DisputeStatus.CLOSED));
        TRANSITIONS.put(DisputeStatus.CLOSED, EnumSet.noneOf(DisputeStatus.class));
    }

    private DisputeStateMachine() {
    }

    /**
     * @return {@code true} if a dispute may move from {@code from} to {@code to}
     */
    public static boolean canTransition(DisputeStatus from, DisputeStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(DisputeStatus.class))
                .contains(to);
    }

    /**
     * Validates a proposed transition, throwing if it is not allowed.
     *
     * @throws InvalidDisputeStateException if the transition is illegal
     */
    public static void assertCanTransition(DisputeStatus from, DisputeStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidDisputeStateException(
                    "Illegal dispute transition: " + from + " -> " + to);
        }
    }
}
