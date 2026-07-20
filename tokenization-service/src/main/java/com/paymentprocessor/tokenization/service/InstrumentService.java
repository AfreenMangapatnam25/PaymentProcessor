package com.paymentprocessor.tokenization.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.tokenization.entity.Instrument;
import com.paymentprocessor.tokenization.repository.InstrumentRepository;

@Service
public class InstrumentService {

    private final InstrumentRepository repository;

    public InstrumentService(InstrumentRepository repository) {
        this.repository = repository;
    }

    public List<Instrument> findAll() {
        return repository.findAll();
    }

    public Optional<Instrument> findById(String id) {
        return repository.findById(id);
    }

    public Instrument save(Instrument entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
