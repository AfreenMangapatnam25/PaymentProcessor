package com.paymentprocessor.fraudservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.paymentprocessor.fraudservice.domain.entity.Device;
import com.paymentprocessor.fraudservice.repository.DeviceRepository;

@Service
public class DeviceService {

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    public List<Device> findAll() {
        return repository.findAll();
    }

    public Optional<Device> findById(UUID id) {
        return repository.findById(id);
    }

    public Device save(Device entity) {
        return repository.save(entity);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
