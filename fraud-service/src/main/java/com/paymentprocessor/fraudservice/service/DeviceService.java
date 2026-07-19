package com.paymentprocessor.fraudservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.fraudservice.document.Device;
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

    public Optional<Device> findById(String id) {
        return repository.findById(id);
    }

    public Device save(Device entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
