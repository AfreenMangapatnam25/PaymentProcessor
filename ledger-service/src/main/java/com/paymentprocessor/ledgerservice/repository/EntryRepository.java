package com.paymentprocessor.ledgerservice.repository;

import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import com.paymentprocessor.ledgerservice.entity.Entry;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findByJournalIdOrderByLineNumberAsc(String journalId);

    List<Entry> findByAccountIdOrderByIdAsc(String accountId);

    Page<Entry> findByAccountIdOrderByIdAsc(String accountId, Pageable pageable);

    List<Entry> findByAccountIdAndEffectiveAtBetweenOrderByIdAsc(
            String accountId, Instant from, Instant to);

    /** Entries in the half-open window (from, to], ordered for statement rendering. */
    List<Entry> findByAccountIdAndEffectiveAtGreaterThanAndEffectiveAtLessThanEqualOrderByIdAsc(
            String accountId, Instant from, Instant to);

    /**
     * Per-account aggregate of debit/credit totals and entry counts for entries
     * effective on or before {@code asOf}. Each row: [accountId, debitMinor,
     * creditMinor, entryCount].
     */
    @Query("select e.accountId, "
            + "coalesce(sum(case when e.direction = :debit then e.amountMinor else 0L end), 0L), "
            + "coalesce(sum(case when e.direction = :credit then e.amountMinor else 0L end), 0L), "
            + "count(e) "
            + "from Entry e where e.effectiveAt <= :asOf group by e.accountId")
    List<Object[]> aggregateByAccountUpTo(@Param("debit") EntryDirection debit,
                                          @Param("credit") EntryDirection credit,
                                          @Param("asOf") Instant asOf);

    /**
     * Aggregate for a single account over a half-open window (from, to].
     * Returns one row: [debitMinor, creditMinor, entryCount, maxEntryId, lastEntryAt].
     */
    @Query("select "
            + "coalesce(sum(case when e.direction = :debit then e.amountMinor else 0L end), 0L), "
            + "coalesce(sum(case when e.direction = :credit then e.amountMinor else 0L end), 0L), "
            + "count(e), coalesce(max(e.id), 0L), max(e.effectiveAt) "
            + "from Entry e where e.accountId = :accountId "
            + "and e.effectiveAt > :from and e.effectiveAt <= :to")
    List<Object[]> aggregateForAccountBetween(@Param("accountId") String accountId,
                                              @Param("debit") EntryDirection debit,
                                              @Param("credit") EntryDirection credit,
                                              @Param("from") Instant from,
                                              @Param("to") Instant to);
}
