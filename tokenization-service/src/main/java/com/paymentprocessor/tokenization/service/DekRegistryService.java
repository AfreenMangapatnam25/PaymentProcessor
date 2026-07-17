package com.paymentprocessor.tokenization.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.tokenization.entity.DekRegistry;
import com.paymentprocessor.tokenization.repository.DekRegistryRepository;

@Service
public class DekRegistryService {

    private final DekRegistryRepository repository;

    public DekRegistryService(DekRegistryRepository repository) {
        this.repository = repository;
    }

    public List<DekRegistry> findAll() {
        return repository.findAll();
    }

    public Optional<DekRegistry> findById(String id) {
        return repository.findById(id);
    }

    public DekRegistry save(DekRegistry entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
