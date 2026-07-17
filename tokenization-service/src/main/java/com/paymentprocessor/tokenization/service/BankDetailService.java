package com.paymentprocessor.tokenization.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.tokenization.entity.BankDetail;
import com.paymentprocessor.tokenization.repository.BankDetailRepository;

@Service
public class BankDetailService {

    private final BankDetailRepository repository;

    public BankDetailService(BankDetailRepository repository) {
        this.repository = repository;
    }

    public List<BankDetail> findAll() {
        return repository.findAll();
    }

    public Optional<BankDetail> findById(String id) {
        return repository.findById(id);
    }

    public BankDetail save(BankDetail entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
