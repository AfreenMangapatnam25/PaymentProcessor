package com.paymentprocessor.fraudservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.fraudservice.document.RiskAssessment;

@Repository
public interface RiskAssessmentRepository extends MongoRepository<RiskAssessment, String> {
}
