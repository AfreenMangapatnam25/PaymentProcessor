package com.paymentprocessor.tokenization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.tokenization.entity.InstrumentAccessLog;

@Repository
public interface InstrumentAccessLogRepository extends JpaRepository<InstrumentAccessLog, Long> {
}
