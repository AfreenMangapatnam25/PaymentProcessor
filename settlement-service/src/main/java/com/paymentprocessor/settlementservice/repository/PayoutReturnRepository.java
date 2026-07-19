package com.paymentprocessor.settlementservice.repository;

import com.paymentprocessor.settlementservice.entity.PayoutReturn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutReturnRepository extends JpaRepository<PayoutReturn, String> {

    List<PayoutReturn> findByPayoutId(String payoutId);
}
