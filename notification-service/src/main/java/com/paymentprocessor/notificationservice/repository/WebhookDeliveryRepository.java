package com.paymentprocessor.notificationservice.repository;

import com.paymentprocessor.notificationservice.entity.WebhookDelivery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByEndpointId(String endpointId);

    /**
     * Atomically claims up to {@code batchSize} due deliveries for processing.
     *
     * Combines the dispatcher's three requirements in one statement:
     *  - FOR UPDATE SKIP LOCKED so concurrent dispatcher instances never grab
     *    the same row (the queue-worker pattern from the design doc).
     *  - The NOT EXISTS clause enforces per-merchant ordering: a delivery is
     *    only claimable if no earlier-sequence delivery to the same endpoint
     *    is still pending/delivering.
     *  - The UPDATE ... RETURNING claims the row (status -> delivering,
     *    attempt += 1, next_retry_at pushed out to a claim-visibility
     *    deadline) in the same statement that selected it, so there's no
     *    window between "select" and "claim" for another worker to race in.
     *
     * next_retry_at doubles as a claim-visibility deadline while a row is
     * 'delivering': if this process crashes mid-delivery, reclaimStuck()
     * resets it back to pending once that deadline passes.
     */
    @Query(value =
            "WITH candidates AS MATERIALIZED ( "
            + "  SELECT wd.id "
            + "  FROM webhook_deliveries wd "
            + "  JOIN events e ON e.id = wd.event_id "
            + "  WHERE wd.status = 'pending' AND wd.next_retry_at <= now() "
            + "    AND NOT EXISTS ( "
            + "      SELECT 1 FROM webhook_deliveries wd2 "
            + "      JOIN events e2 ON e2.id = wd2.event_id "
            + "      WHERE wd2.endpoint_id = wd.endpoint_id "
            + "        AND wd2.status IN ('pending', 'delivering') "
            + "        AND e2.sequence < e.sequence "
            + "    ) "
            + "  ORDER BY wd.next_retry_at "
            + "  LIMIT :batchSize "
            + "  FOR UPDATE OF wd SKIP LOCKED "
            + ") "
            + "UPDATE webhook_deliveries wd "
            + "SET status = 'delivering', attempt = wd.attempt + 1, "
            + "    next_retry_at = now() + (:claimVisibilitySeconds || ' seconds')::interval "
            + "FROM candidates c "
            + "WHERE wd.id = c.id "
            + "RETURNING wd.*",
            nativeQuery = true)
    List<WebhookDelivery> claimBatch(@Param("batchSize") int batchSize,
                                      @Param("claimVisibilitySeconds") long claimVisibilitySeconds);

    /** Crash recovery: rows left 'delivering' past their claim deadline go back to pending. */
    @Modifying
    @Query(value = "UPDATE webhook_deliveries SET status = 'pending', next_retry_at = now() "
            + "WHERE status = 'delivering' AND next_retry_at <= now()", nativeQuery = true)
    int reclaimStuck();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM webhook_deliveries wd JOIN events e ON e.id = wd.event_id "
            + "WHERE wd.endpoint_id = :endpointId AND wd.status IN ('pending','delivering') AND e.sequence < :sequence)",
            nativeQuery = true)
    boolean existsEarlierUnresolvedDelivery(@Param("endpointId") String endpointId, @Param("sequence") long sequence);

    long countByEndpointIdAndStatus(String endpointId, String status);
}
