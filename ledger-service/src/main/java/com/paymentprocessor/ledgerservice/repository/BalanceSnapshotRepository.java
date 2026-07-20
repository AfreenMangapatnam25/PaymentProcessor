package com.paymentprocessor.ledgerservice.repository;

import com.paymentprocessor.ledgerservice.entity.BalanceSnapshot;
import com.paymentprocessor.ledgerservice.entity.BalanceSnapshotId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceSnapshotRepository extends JpaRepository<BalanceSnapshot, BalanceSnapshotId> {

    List<BalanceSnapshot> findByAccountIdOrderByAsOfDateAsc(String accountId);

    List<BalanceSnapshot> findByAsOfDate(LocalDate asOfDate);

    /** Most recent snapshot for an account strictly before the given date. */
    Optional<BalanceSnapshot> findFirstByAccountIdAndAsOfDateBeforeOrderByAsOfDateDesc(
            String accountId, LocalDate asOfDate);
}
