package com.paymentprocessor.analytics.repository;

import com.paymentprocessor.analytics.domain.entity.ReportJob;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportJobRepository extends JpaRepository<ReportJob, UUID> {

    Page<ReportJob> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    long countByMerchantIdAndStatusIn(String merchantId, List<ReportStatus> statuses);

    List<ReportJob> findByStatusAndExpiresAtBefore(ReportStatus status, Instant cutoff);

    @Modifying
    @Query("update ReportJob j set j.status = com.paymentprocessor.analytics.domain.enums.ReportStatus.EXPIRED "
            + "where j.status = com.paymentprocessor.analytics.domain.enums.ReportStatus.COMPLETED "
            + "and j.expiresAt < :cutoff")
    int expireCompletedBefore(@Param("cutoff") Instant cutoff);
}
