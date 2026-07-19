package com.paymentprocessor.fraudservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.fraudservice.document.FraudCase;
import com.paymentprocessor.fraudservice.repository.FraudCaseRepository;

@Service
public class FraudCaseService {

    private final FraudCaseRepository repository;

    public FraudCaseService(FraudCaseRepository repository) {
        this.repository = repository;
    }

    public List<FraudCase> findAll() {
        return repository.findAll();
    }

    public Optional<FraudCase> findById(String id) {
        return repository.findById(id);
    }

    public FraudCase save(FraudCase entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
