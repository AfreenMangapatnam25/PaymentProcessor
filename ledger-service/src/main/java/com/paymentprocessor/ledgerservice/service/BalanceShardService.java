package com.paymentprocessor.ledgerservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.ledgerservice.entity.BalanceShard;
import com.paymentprocessor.ledgerservice.repository.BalanceShardRepository;
import com.paymentprocessor.ledgerservice.entity.BalanceShardId;

@Service
public class BalanceShardService {

    private final BalanceShardRepository repository;

    public BalanceShardService(BalanceShardRepository repository) {
        this.repository = repository;
    }

    public List<BalanceShard> findAll() {
        return repository.findAll();
    }

    public Optional<BalanceShard> findById(BalanceShardId id) {
        return repository.findById(id);
    }

    public BalanceShard save(BalanceShard entity) {
        return repository.save(entity);
    }

    public void deleteById(BalanceShardId id) {
        repository.deleteById(id);
    }
}
