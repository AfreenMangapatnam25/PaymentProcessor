package com.paymentprocessor.disputeservice.dto.response;

import com.paymentprocessor.disputeservice.domain.enums.DisputeSource;
import com.paymentprocessor.disputeservice.domain.enums.DisputeStage;
import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.domain.enums.DisputeType;
import com.paymentprocessor.disputeservice.domain.enums.LiabilityParty;
import com.paymentprocessor.disputeservice.domain.enums.Network;
import com.paymentprocessor.disputeservice.entity.Dispute;
import java.time.Instant;

/**
 * API view of a dispute.
 */
public record DisputeResponse(
        String id,
        String chargebackId,
        String transactionId,
        String paymentId,
        String merchantId,
        String customerId,
        Network network,
        DisputeType type,
        DisputeSource source,
        DisputeStage stage,
        DisputeStatus status,
        String reasonCode,
        String reasonDescription,
        Long amountMinor,
        String currency,
        Long chargebackFeeMinor,
        boolean partial,
        LiabilityParty liabilityParty,
        Instant receivedAt,
        Instant openedAt,
        Instant deadlineAt,
        Instant resolvedAt,
        Instant closedAt,
        boolean merchantNotified,
        long daysUntilDeadline) {

    public static DisputeResponse from(Dispute d) {
        return new DisputeResponse(
                d.getId(), d.getChargebackId(), d.getTransactionId(), d.getPaymentId(),
                d.getMerchantId(), d.getCustomerId(), d.getNetwork(), d.getType(), d.getSource(),
                d.getStage(), d.getStatus(), d.getReasonCode(), d.getReasonDescription(),
                d.getAmountMinor(), d.getCurrency(), d.getChargebackFeeMinor(), d.isPartial(),
                d.getLiabilityParty(), d.getReceivedAt(), d.getOpenedAt(), d.getDeadlineAt(),
                d.getResolvedAt(), d.getClosedAt(), d.isMerchantNotified(),
                d.daysUntilDeadline(Instant.now()));
    }
}
