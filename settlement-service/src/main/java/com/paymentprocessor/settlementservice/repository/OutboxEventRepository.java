package com.paymentprocessor.settlementservice.repository;

import com.paymentprocessor.settlementservice.entity.OutboxEvent;
import com.paymentprocessor.settlementservice.enums.OutboxStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByIdAsc(OutboxStatus status, Pageable pageable);
}
