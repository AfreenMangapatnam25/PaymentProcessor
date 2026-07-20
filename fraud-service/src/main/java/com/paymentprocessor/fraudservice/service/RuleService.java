package com.paymentprocessor.fraudservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.paymentprocessor.fraudservice.domain.entity.Rule;
import com.paymentprocessor.fraudservice.repository.RuleRepository;

@Service
public class RuleService {

    private final RuleRepository repository;

    public RuleService(RuleRepository repository) {
        this.repository = repository;
    }

    public List<Rule> findAll() {
        return repository.findAll();
    }

    public Optional<Rule> findById(UUID id) {
        return repository.findById(id);
    }

    public Rule save(Rule entity) {
        return repository.save(entity);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
