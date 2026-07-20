package com.paymentprocessor.fraudservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.paymentprocessor.fraudservice.domain.entity.ModelRegistry;
import com.paymentprocessor.fraudservice.repository.ModelRegistryRepository;

@Service
public class ModelRegistryService {

    private final ModelRegistryRepository repository;

    public ModelRegistryService(ModelRegistryRepository repository) {
        this.repository = repository;
    }

    public List<ModelRegistry> findAll() {
        return repository.findAll();
    }

    public Optional<ModelRegistry> findById(UUID id) {
        return repository.findById(id);
    }

    public ModelRegistry save(ModelRegistry entity) {
        return repository.save(entity);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
