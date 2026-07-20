package com.paymentprocessor.ledgerservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.ledgerservice.entity.BalanceShard;
import com.paymentprocessor.ledgerservice.entity.BalanceShardId;

@Repository
public interface BalanceShardRepository extends JpaRepository<BalanceShard, BalanceShardId> {
}
