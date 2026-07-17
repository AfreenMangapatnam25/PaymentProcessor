package com.paymentprocessor.tokenization.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.tokenization.entity.NetworkToken;
import com.paymentprocessor.tokenization.repository.NetworkTokenRepository;

@Service
public class NetworkTokenService {

    private final NetworkTokenRepository repository;

    public NetworkTokenService(NetworkTokenRepository repository) {
        this.repository = repository;
    }

    public List<NetworkToken> findAll() {
        return repository.findAll();
    }

    public Optional<NetworkToken> findById(String id) {
        return repository.findById(id);
    }

    public NetworkToken save(NetworkToken entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
