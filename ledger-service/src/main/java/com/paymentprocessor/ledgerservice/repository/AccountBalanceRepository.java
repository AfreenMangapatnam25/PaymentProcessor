package com.paymentprocessor.ledgerservice.repository;

import com.paymentprocessor.ledgerservice.entity.AccountBalance;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, String> {

    /**
     * Load a balance row with a pessimistic write lock (SELECT ... FOR UPDATE),
     * serialising concurrent postings to the same account.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from AccountBalance b where b.accountId = :accountId")
    Optional<AccountBalance> findByIdForUpdate(@Param("accountId") String accountId);
}
