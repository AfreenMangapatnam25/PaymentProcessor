package com.paymentprocessor.settlementservice.integration.rail;

import com.paymentprocessor.settlementservice.enums.FailureCategory;
import com.paymentprocessor.settlementservice.exception.RailException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process stand-in for real banking rails. Deterministically simulates
 * success and the full range of failure categories based on markers in the
 * destination account id, so the retry/exception paths can be exercised
 * end-to-end without a real bank:
 *
 * <ul>
 *   <li>account containing "invalid" / "closed" -&gt; MERCHANT_SIDE (no retry)</li>
 *   <li>account containing "sanction"           -&gt; FATAL (escalate)</li>
 *   <li>account containing "timeout"            -&gt; TRANSIENT (retry w/ backoff)</li>
 *   <li>account containing "nsf"                -&gt; RECOVERABLE (retry next day)</li>
 *   <li>otherwise                               -&gt; accepted</li>
 * </ul>
 */
@Component
public class SimulatedRailGateway implements RailGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedRailGateway.class);

    @Override
    public RailAck submit(RailTransferRequest request) throws RailException {
        String account = request.payoutAccountId() == null
                ? "" : request.payoutAccountId().toLowerCase();

        if (account.contains("invalid") || account.contains("closed")) {
            throw new RailException("R03", FailureCategory.MERCHANT_SIDE,
                    "No account/unable to locate account");
        }
        if (account.contains("sanction")) {
            throw new RailException("SANCTIONS_HIT", FailureCategory.FATAL,
                    "Beneficiary matched a sanctions list");
        }
        if (account.contains("timeout")) {
            throw new RailException("RAIL_TIMEOUT", FailureCategory.TRANSIENT,
                    "Rail did not respond in time");
        }
        if (account.contains("nsf")) {
            throw new RailException("NSF", FailureCategory.RECOVERABLE,
                    "Insufficient platform funds");
        }

        String providerRef = request.rail().name().toLowerCase() + "_" + UUID.randomUUID();
        log.info("Rail [{}] accepted transfer payout={} amount={} {} ref={}",
                request.rail(), request.payoutId(), request.amountMinor(),
                request.currency(), providerRef);

        // Instant rails settle synchronously; batch rails go into processing.
        boolean processing = !request.rail().isInstant();
        return new RailAck(providerRef, processing);
    }
}
