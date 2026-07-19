package com.paymentprocessor.fraudservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.paymentprocessor.fraudservice.domain.entity.ListEntry;
import com.paymentprocessor.fraudservice.repository.ListEntryRepository;

@Service
public class ListEntryService {

    private final ListEntryRepository repository;

    public ListEntryService(ListEntryRepository repository) {
        this.repository = repository;
    }

    public List<ListEntry> findAll() {
        return repository.findAll();
    }

    public Optional<ListEntry> findById(UUID id) {
        return repository.findById(id);
    }

    public ListEntry save(ListEntry entity) {
        return repository.save(entity);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
