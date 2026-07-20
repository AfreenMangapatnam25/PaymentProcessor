package com.paymentprocessor.settlementservice.service.batch;

import com.paymentprocessor.settlementservice.config.SettlementProperties;
import com.paymentprocessor.settlementservice.entity.Adjustment;
import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.entity.SettlementItem;
import com.paymentprocessor.settlementservice.enums.AdjustmentType;
import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.enums.Rail;
import com.paymentprocessor.settlementservice.enums.ScheduleType;
import com.paymentprocessor.settlementservice.enums.SettlementItemType;
import com.paymentprocessor.settlementservice.integration.merchant.MerchantClient;
import com.paymentprocessor.settlementservice.integration.merchant.MerchantSettlementProfile;
import com.paymentprocessor.settlementservice.integration.rail.RailRouter;
import com.paymentprocessor.settlementservice.repository.PayoutRepository;
import com.paymentprocessor.settlementservice.repository.SettlementBatchRepository;
import com.paymentprocessor.settlementservice.repository.SettlementItemRepository;
import com.paymentprocessor.settlementservice.service.AdjustmentService;
import com.paymentprocessor.settlementservice.service.ReserveService;
import com.paymentprocessor.settlementservice.service.calculation.CalculationResult;
import com.paymentprocessor.settlementservice.service.calculation.CalculationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates a merchant's unbatched settlement items and approved adjustments
 * into a settlement batch, runs the calculation engine, holds the rolling
 * reserve, and creates the payout instruction.
 *
 * <p>If the resulting net is below the configured minimum payout (or non-positive,
 * i.e. the merchant owes the platform), the items are left unbatched so they roll
 * into the next cycle and naturally offset against future settlements.
 */
@Service
public class BatchingService {

    private static final Logger log = LoggerFactory.getLogger(BatchingService.class);

    private final SettlementItemRepository itemRepository;
    private final SettlementBatchRepository batchRepository;
    private final PayoutRepository payoutRepository;
    private final MerchantClient merchantClient;
    private final CalculationService calculationService;
    private final RailRouter railRouter;
    private final ReserveService reserveService;
    private final AdjustmentService adjustmentService;
    private final SettlementProperties properties;

    public BatchingService(SettlementItemRepository itemRepository,
                           SettlementBatchRepository batchRepository,
                           PayoutRepository payoutRepository,
                           MerchantClient merchantClient,
                           CalculationService calculationService,
                           RailRouter railRouter,
                           ReserveService reserveService,
                           AdjustmentService adjustmentService,
                           SettlementProperties properties) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.payoutRepository = payoutRepository;
        this.merchantClient = merchantClient;
        this.calculationService = calculationService;
        this.railRouter = railRouter;
        this.reserveService = reserveService;
        this.adjustmentService = adjustmentService;
        this.properties = properties;
    }

    /**
     * Builds a settlement batch for the given merchant + currency from all
     * currently unbatched items and approved adjustments.
     *
     * @return the created batch, or empty if there was nothing to settle or the
     *         net fell below the minimum payout threshold (rolled over).
     */
    @Transactional
    public Optional<SettlementBatch> createBatch(String merchantId, String currency, ScheduleType scheduleType) {
        MerchantSettlementProfile profile = merchantClient.getProfile(merchantId)
                .orElseThrow(() -> new IllegalStateException("Unknown merchant profile: " + merchantId));
        if (!profile.active()) {
            log.warn("Skipping settlement for inactive merchant {}", merchantId);
            return Optional.empty();
        }

        List<SettlementItem> items = itemRepository
                .findByMerchantIdAndCurrencyAndBatchIdIsNull(merchantId, currency);
        List<Adjustment> adjustments = adjustmentService.approvedFor(merchantId, currency);

        if (items.isEmpty() && adjustments.isEmpty()) {
            return Optional.empty();
        }

        // Build transient items for the approved adjustments to feed the calculator.
        List<SettlementItem> adjustmentItems = new ArrayList<>();
        for (Adjustment adj : adjustments) {
            adjustmentItems.add(toTransientItem(adj));
        }

        List<SettlementItem> combined = new ArrayList<>(items);
        combined.addAll(adjustmentItems);

        // Pass 1: net excluding the (yet unknown) rail fee, to choose a rail.
        CalculationResult preliminary = calculationService.calculate(combined, profile, 0L);
        Rail rail = railRouter.selectRail(currency, preliminary.netMinor(),
                profile.instantPayout(), profile.preferredRail());
        long railFee = rail.getDefaultFeeMinor();

        // Pass 2: final net including the rail transfer fee.
        CalculationResult result = calculationService.calculate(combined, profile, railFee);

        if (result.netMinor() < properties.getMinimumPayoutMinor()) {
            log.info("Merchant {} net {} {} below minimum {} -> rolled over to next cycle",
                    merchantId, result.netMinor(), currency, properties.getMinimumPayoutMinor());
            return Optional.empty();
        }

        SettlementBatch batch = persistBatch(merchantId, currency, scheduleType, items, result);

        // Assign real items and persist adjustment items to the batch.
        for (SettlementItem item : items) {
            item.setBatchId(batch.getId());
        }
        itemRepository.saveAll(items);
        for (int i = 0; i < adjustments.size(); i++) {
            SettlementItem ai = adjustmentItems.get(i);
            ai.setBatchId(batch.getId());
            itemRepository.save(ai);
            adjustmentService.markApplied(adjustments.get(i), batch.getId());
        }

        // Hold the rolling reserve.
        if (result.reserveMinor() > 0) {
            reserveService.createHold(merchantId, currency, result.reserveMinor(),
                    profile.reserveRateBps(), batch.getId(), profile.reserveHoldDays());
        }

        createPayout(batch, profile, rail, result.netMinor());

        log.info("Created batch {} for merchant {}: gross={} net={} rail={} {}",
                batch.getId(), merchantId, result.grossMinor(), result.netMinor(), rail, currency);
        return Optional.of(batch);
    }

    private SettlementBatch persistBatch(String merchantId, String currency, ScheduleType scheduleType,
                                         List<SettlementItem> items, CalculationResult result) {
        SettlementBatch batch = new SettlementBatch();
        batch.setId("batch_" + UUID.randomUUID());
        batch.setMerchantId(merchantId);
        batch.setCurrency(currency);
        batch.setScheduleType(scheduleType);
        batch.setPeriodStart(items.stream().map(SettlementItem::getEffectiveAt)
                .filter(java.util.Objects::nonNull).min(Comparator.naturalOrder()).orElse(Instant.now()));
        batch.setPeriodEnd(items.stream().map(SettlementItem::getEffectiveAt)
                .filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(Instant.now()));
        batch.setStatus(BatchStatus.PENDING);
        batch.setGrossMinor(result.grossMinor());
        batch.setRefundsMinor(result.refundsMinor());
        batch.setFeesMinor(result.platformFeeMinor());
        batch.setInterchangeMinor(result.interchangeMinor());
        batch.setChargebacksMinor(result.chargebacksMinor());
        batch.setAdjustmentsMinor(result.adjustmentsMinor());
        batch.setReserveMinor(result.reserveMinor());
        batch.setSettlementFeeMinor(result.settlementFeeMinor());
        batch.setNetMinor(result.netMinor());
        return batchRepository.save(batch);
    }

    private void createPayout(SettlementBatch batch, MerchantSettlementProfile profile,
                              Rail rail, long netMinor) {
        Payout payout = new Payout();
        payout.setId("payout_" + UUID.randomUUID());
        payout.setBatchId(batch.getId());
        payout.setMerchantId(batch.getMerchantId());
        payout.setPayoutAccountId(profile.payoutAccountId());
        payout.setAmountMinor(netMinor);
        payout.setCurrency(batch.getCurrency());
        payout.setRail(rail);
        payout.setStatus(PayoutStatus.PENDING);
        payout.setIdempotencyKey("payout:" + batch.getId());
        payout.setScheduledAt(Instant.now());
        payoutRepository.save(payout);
    }

    private SettlementItem toTransientItem(Adjustment adj) {
        SettlementItem item = new SettlementItem();
        item.setMerchantId(adj.getMerchantId());
        item.setType(mapAdjustmentType(adj.getType()));
        item.setSourceType("ADJUSTMENT");
        item.setSourceId(adj.getId());
        item.setAmountMinor(adj.getAmountMinor());
        item.setCurrency(adj.getCurrency());
        item.setEffectiveAt(Instant.now());
        item.setIdempotencyKey("adjustment:" + adj.getId());
        return item;
    }

    private SettlementItemType mapAdjustmentType(AdjustmentType type) {
        return switch (type) {
            case CREDIT, FEE_CORRECTION, CURRENCY_CORRECTION -> SettlementItemType.ADJUSTMENT_CREDIT;
            case DEBIT, RESERVE_HOLD -> SettlementItemType.ADJUSTMENT_DEBIT;
            case RESERVE_RELEASE -> SettlementItemType.RESERVE_RELEASE;
        };
    }
}
