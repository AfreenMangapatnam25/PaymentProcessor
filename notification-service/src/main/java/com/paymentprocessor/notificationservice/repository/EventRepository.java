package com.paymentprocessor.notificationservice.repository;

import com.paymentprocessor.notificationservice.entity.Event;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findByMerchantIdOrderBySequenceDesc(String merchantId, Pageable pageable);

    Optional<Event> findByIdAndCreatedAt(String id, Instant createdAt);

    @Query("select coalesce(max(e.sequence), 0) from Event e where e.merchantId = :merchantId")
    long findMaxSequence(@Param("merchantId") String merchantId);
}
