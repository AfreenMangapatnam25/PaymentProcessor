package com.paymentprocessor.ledgerservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.ledgerservice.entity.AccountType;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, String> {
}
