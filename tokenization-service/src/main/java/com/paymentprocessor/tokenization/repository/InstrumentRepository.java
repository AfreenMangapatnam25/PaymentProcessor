package com.paymentprocessor.tokenization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.tokenization.entity.Instrument;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, String> {
}
