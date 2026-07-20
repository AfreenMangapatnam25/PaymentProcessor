package com.paymentprocessor.tokenization.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.tokenization.entity.InstrumentAccessLog;
import com.paymentprocessor.tokenization.repository.InstrumentAccessLogRepository;

@Service
public class InstrumentAccessLogService {

    private final InstrumentAccessLogRepository repository;

    public InstrumentAccessLogService(InstrumentAccessLogRepository repository) {
        this.repository = repository;
    }

    public List<InstrumentAccessLog> findAll() {
        return repository.findAll();
    }

    public Optional<InstrumentAccessLog> findById(Long id) {
        return repository.findById(id);
    }

    public InstrumentAccessLog save(InstrumentAccessLog entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
