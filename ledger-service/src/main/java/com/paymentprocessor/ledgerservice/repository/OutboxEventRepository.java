package com.paymentprocessor.ledgerservice.repository;

import com.paymentprocessor.ledgerservice.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);

    long countByPublishedFalse();
}
