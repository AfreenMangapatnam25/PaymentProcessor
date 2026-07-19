package com.paymentprocessor.fraudservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.fraudservice.document.ListEntry;
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

    public Optional<ListEntry> findById(String id) {
        return repository.findById(id);
    }

    public ListEntry save(ListEntry entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
