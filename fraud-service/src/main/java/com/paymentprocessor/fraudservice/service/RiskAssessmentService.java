package com.paymentprocessor.fraudservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.fraudservice.document.RiskAssessment;
import com.paymentprocessor.fraudservice.repository.RiskAssessmentRepository;

@Service
public class RiskAssessmentService {

    private final RiskAssessmentRepository repository;

    public RiskAssessmentService(RiskAssessmentRepository repository) {
        this.repository = repository;
    }

    public List<RiskAssessment> findAll() {
        return repository.findAll();
    }

    public Optional<RiskAssessment> findById(String id) {
        return repository.findById(id);
    }

    public RiskAssessment save(RiskAssessment entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
