package com.paymentprocessor.ledgerservice.repository;

import com.paymentprocessor.ledgerservice.domain.enums.HoldStatus;
import com.paymentprocessor.ledgerservice.entity.Hold;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoldRepository extends JpaRepository<Hold, String> {

    List<Hold> findByAccountIdAndStatus(String accountId, HoldStatus status);

    List<Hold> findByAccountId(String accountId);

    List<Hold> findByExternalRef(String externalRef);
}
