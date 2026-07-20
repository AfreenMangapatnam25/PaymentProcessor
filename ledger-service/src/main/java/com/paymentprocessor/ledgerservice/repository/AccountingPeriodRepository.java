package com.paymentprocessor.ledgerservice.repository;

import com.paymentprocessor.ledgerservice.entity.AccountingPeriod;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, String> {

    Optional<AccountingPeriod> findByCode(String code);

    /** Periods whose date range contains the given date (usually zero or one). */
    List<AccountingPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate start, LocalDate end);
}
