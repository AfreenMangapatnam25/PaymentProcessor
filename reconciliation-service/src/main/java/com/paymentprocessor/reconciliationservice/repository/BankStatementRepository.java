package com.paymentprocessor.reconciliationservice.repository;

import com.paymentprocessor.reconciliationservice.domain.BankStatement;
import com.paymentprocessor.reconciliationservice.domain.StatementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {

    Optional<BankStatement> findByStatementReference(String statementReference);

    boolean existsByStatementReference(String statementReference);

    Page<BankStatement> findByStatus(StatementStatus status, Pageable pageable);
}
