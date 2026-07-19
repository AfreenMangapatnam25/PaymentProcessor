package com.paymentprocessor.limit.repository;

import com.paymentprocessor.limit.domain.entity.ReservationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationLineRepository extends JpaRepository<ReservationLine, UUID> {

    List<ReservationLine> findByReservationId(UUID reservationId);
}
