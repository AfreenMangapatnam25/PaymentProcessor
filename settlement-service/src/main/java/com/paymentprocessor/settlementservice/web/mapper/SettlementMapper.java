package com.paymentprocessor.settlementservice.web.mapper;

import com.paymentprocessor.settlementservice.entity.Adjustment;
import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.entity.Reserve;
import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.entity.SettlementItem;
import com.paymentprocessor.settlementservice.web.dto.AdjustmentResponse;
import com.paymentprocessor.settlementservice.web.dto.BatchResponse;
import com.paymentprocessor.settlementservice.web.dto.ItemResponse;
import com.paymentprocessor.settlementservice.web.dto.PayoutResponse;
import com.paymentprocessor.settlementservice.web.dto.ReserveResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps persistent entities to their API response representations. */
@Component
public class SettlementMapper {

    public BatchResponse toBatchResponse(SettlementBatch b) {
        return new BatchResponse(
                b.getId(), b.getMerchantId(), b.getCurrency(), b.getScheduleType(), b.getStatus(),
                b.getPeriodStart(), b.getPeriodEnd(), b.getGrossMinor(), b.getRefundsMinor(),
                b.getChargebacksMinor(), b.getFeesMinor(), b.getInterchangeMinor(), b.getReserveMinor(),
                b.getSettlementFeeMinor(), b.getAdjustmentsMinor(), b.getNetMinor(), b.getLedgerJournalId(),
                b.getApprovedBy(), b.getApprovedAt(), b.getClosedAt(), b.getFailureReason(),
                b.getCreatedAt(), b.getUpdatedAt());
    }

    public PayoutResponse toPayoutResponse(Payout p) {
        return new PayoutResponse(
                p.getId(), p.getBatchId(), p.getMerchantId(), p.getPayoutAccountId(), p.getAmountMinor(),
                p.getCurrency(), p.getRail(), p.getStatus(), p.getProviderRef(), p.getLedgerJournalId(),
                p.getAttemptCount(), p.getNextRetryAt(), p.getFailureCode(), p.getFailureReason(),
                p.getFailureCategory(), p.getScheduledAt(), p.getSubmittedAt(), p.getPaidAt(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    public ReserveResponse toReserveResponse(Reserve r) {
        return new ReserveResponse(
                r.getId(), r.getMerchantId(), r.getKind(), r.getRateBps(), r.getAmountMinor(),
                r.getReleasedMinor(), r.remainingMinor(), r.getCurrency(), r.getSourceBatchId(),
                r.getHoldUntil(), r.getStatus(), r.getReleasedAt(), r.getCreatedAt());
    }

    public AdjustmentResponse toAdjustmentResponse(Adjustment a) {
        return new AdjustmentResponse(
                a.getId(), a.getMerchantId(), a.getType(), a.getAmountMinor(), a.getCurrency(),
                a.getReasonCode(), a.getDescription(), a.getStatus(), a.getRequiredApprovalLevel(),
                a.getRequestedBy(), a.getApprovedBy(), a.getApprovedAt(), a.getAppliedBatchId(),
                a.getAppliedAt(), a.getCreatedAt());
    }

    public ItemResponse toItemResponse(SettlementItem i) {
        return new ItemResponse(
                i.getId(), i.getBatchId(), i.getMerchantId(), i.getType(), i.getSourceType(),
                i.getSourceId(), i.getAmountMinor(), i.signedAmountMinor(), i.getCurrency(), i.getEffectiveAt());
    }

    public List<BatchResponse> toBatchResponses(List<SettlementBatch> batches) {
        return batches.stream().map(this::toBatchResponse).toList();
    }

    public List<PayoutResponse> toPayoutResponses(List<Payout> payouts) {
        return payouts.stream().map(this::toPayoutResponse).toList();
    }

    public List<ReserveResponse> toReserveResponses(List<Reserve> reserves) {
        return reserves.stream().map(this::toReserveResponse).toList();
    }

    public List<AdjustmentResponse> toAdjustmentResponses(List<Adjustment> adjustments) {
        return adjustments.stream().map(this::toAdjustmentResponse).toList();
    }

    public List<ItemResponse> toItemResponses(List<SettlementItem> items) {
        return items.stream().map(this::toItemResponse).toList();
    }
}
