package com.paymentprocessor.tokenization.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.tokenization.entity.BinRange;
import com.paymentprocessor.tokenization.repository.BinRangeRepository;

@Service
public class BinRangeService {

    private final BinRangeRepository repository;

    public BinRangeService(BinRangeRepository repository) {
        this.repository = repository;
    }

    public List<BinRange> findAll() {
        return repository.findAll();
    }

    public Optional<BinRange> findById(Long id) {
        return repository.findById(id);
    }

    public BinRange save(BinRange entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
