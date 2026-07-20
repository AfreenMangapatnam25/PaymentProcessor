package com.paymentprocessor.ledgerservice.repository;

import com.paymentprocessor.ledgerservice.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountCode(String accountCode);

    boolean existsByAccountCode(String accountCode);

    List<Account> findByOwnerTypeAndOwnerId(String ownerType, String ownerId);

    List<Account> findByTypeCode(String typeCode);
}
