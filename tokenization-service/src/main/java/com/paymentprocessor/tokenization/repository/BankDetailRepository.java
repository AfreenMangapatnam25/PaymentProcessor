package com.paymentprocessor.tokenization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.tokenization.entity.BankDetail;

@Repository
public interface BankDetailRepository extends JpaRepository<BankDetail, String> {
}
