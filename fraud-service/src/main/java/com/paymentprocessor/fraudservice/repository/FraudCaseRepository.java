package com.paymentprocessor.fraudservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.fraudservice.document.FraudCase;

@Repository
public interface FraudCaseRepository extends MongoRepository<FraudCase, String> {
}
