package com.paymentprocessor.fraudservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.paymentprocessor.fraudservice.domain.entity.FraudCase;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {
}
