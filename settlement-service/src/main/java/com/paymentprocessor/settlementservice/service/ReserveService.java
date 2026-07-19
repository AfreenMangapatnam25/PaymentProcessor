package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.entity.Reserve;
import com.paymentprocessor.settlementservice.entity.SettlementItem;
import com.paymentprocessor.settlementservice.enums.ReserveKind;
import com.paymentprocessor.settlementservice.enums.ReserveStatus;
import com.paymentprocessor.settlementservice.enums.SettlementItemType;
import com.paymentprocessor.settlementservice.exception.ResourceNotFoundException;
import com.paymentprocessor.settlementservice.integration.events.DomainEventPublisher;
import com.paymentprocessor.settlementservice.integration.events.EventTypes;
import com.paymentprocessor.settlementservice.integration.ledger.LedgerClient;
import com.paymentprocessor.settlementservice.repository.ReserveRepository;
import com.paymentprocessor.settlementservice.repository.SettlementItemRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages rolling / risk reserves: holding a portion of each settlement and
 * releasing it back into a future settlement once the hold period elapses.
 */
@Service
public class ReserveService {

    private static final Logger log = LoggerFactory.getLogger(ReserveService.class);

    private final ReserveRepository reserveRepository;
    private final SettlementItemRepository itemRepository;
    private final LedgerClient ledgerClient;
    private final DomainEventPublisher eventPublisher;

    public ReserveService(ReserveRepository reserveRepository,
                          SettlementItemRepository itemRepository,
                          LedgerClient ledgerClient,
                          DomainEventPublisher eventPublisher) {
        this.reserveRepository = reserveRepository;
        this.itemRepository = itemRepository;
        this.ledgerClient = ledgerClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Records a reserve hold created during batch calculation and posts the
     * corresponding ledger hold.
     */
    @Transactional
    public Reserve createHold(String merchantId, String currency, long amountMinor,
                              Integer rateBps, String sourceBatchId, int holdDays) {
        Reserve reserve = new Reserve();
        reserve.setId("rsv_" + UUID.randomUUID());
        reserve.setMerchantId(merchantId);
        reserve.setKind(ReserveKind.ROLLING);
        reserve.setRateBps(rateBps);
        reserve.setAmountMinor(amountMinor);
        reserve.setReleasedMinor(0L);
        reserve.setCurrency(currency);
        reserve.setSourceBatchId(sourceBatchId);
        reserve.setHoldUntil(LocalDate.now().plusDays(holdDays));
        reserve.setStatus(ReserveStatus.HELD);
        reserve.setLedgerHoldId(ledgerClient.postReserveHold(reserve.getId(), merchantId, currency, amountMinor));
        Reserve saved = reserveRepository.save(reserve);
        log.info("Held reserve {} for merchant {} amount {} {} until {}",
                saved.getId(), merchantId, amountMinor, currency, saved.getHoldUntil());
        return saved;
    }

    /**
     * Releases every reserve whose hold period has elapsed. Each release posts a
     * ledger entry, emits a {@code RESERVE_RELEASE} settlement item so the funds
     * flow into the merchant's next batch, and publishes a domain event.
     *
     * @return the number of reserves released
     */
    @Transactional
    public int releaseDueReserves(LocalDate asOf) {
        List<Reserve> due = reserveRepository.findByStatusAndHoldUntilLessThanEqual(ReserveStatus.HELD, asOf);
        for (Reserve reserve : due) {
            releaseReserve(reserve);
        }
        if (!due.isEmpty()) {
            log.info("Released {} due reserves as of {}", due.size(), asOf);
        }
        return due.size();
    }

    private void releaseReserve(Reserve reserve) {
        long remaining = reserve.remainingMinor();
        reserve.setLedgerHoldId(ledgerClient.postReserveRelease(
                reserve.getId(), reserve.getMerchantId(), reserve.getCurrency(), remaining));
        reserve.setReleasedMinor(reserve.getAmountMinor());
        reserve.setStatus(ReserveStatus.RELEASED);
        reserve.setReleasedAt(Instant.now());
        reserveRepository.save(reserve);

        // Feed the released amount into the next settlement as a credit item.
        SettlementItem release = new SettlementItem();
        release.setMerchantId(reserve.getMerchantId());
        release.setType(SettlementItemType.RESERVE_RELEASE);
        release.setSourceType("RESERVE");
        release.setSourceId(reserve.getId());
        release.setAmountMinor(remaining);
        release.setCurrency(reserve.getCurrency());
        release.setEffectiveAt(Instant.now());
        release.setIdempotencyKey("reserve-release:" + reserve.getId());
        itemRepository.save(release);

        eventPublisher.publish(EventTypes.RESERVE_RELEASED, "Reserve", reserve.getId(),
                Map.of("reserveId", reserve.getId(),
                        "merchantId", reserve.getMerchantId(),
                        "amountMinor", remaining,
                        "currency", reserve.getCurrency()));
    }

    @Transactional(readOnly = true)
    public List<Reserve> findByMerchant(String merchantId) {
        return reserveRepository.findByMerchantId(merchantId);
    }

    @Transactional(readOnly = true)
    public Reserve findById(String id) {
        return reserveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserve", id));
    }

    @Transactional(readOnly = true)
    public List<Reserve> findAll() {
        return reserveRepository.findAll();
    }
}
