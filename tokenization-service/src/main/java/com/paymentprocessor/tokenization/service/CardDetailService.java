package com.paymentprocessor.tokenization.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.paymentprocessor.tokenization.entity.CardDetail;
import com.paymentprocessor.tokenization.repository.CardDetailRepository;

@Service
public class CardDetailService {

    private final CardDetailRepository repository;

    public CardDetailService(CardDetailRepository repository) {
        this.repository = repository;
    }

    public List<CardDetail> findAll() {
        return repository.findAll();
    }

    public Optional<CardDetail> findById(String id) {
        return repository.findById(id);
    }

    public CardDetail save(CardDetail entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
