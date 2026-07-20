package com.paymentprocessor.analytics.repository;

import com.paymentprocessor.analytics.domain.entity.ScheduledReport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, UUID> {

    Page<ScheduledReport> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    List<ScheduledReport> findByEnabledTrueAndNextRunAtLessThanEqual(Instant now);
}
