package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.entity.Currency;
import com.paymentprocessor.ledgerservice.repository.CurrencyRepository;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to supported currencies.
 */
@Service
@Transactional(readOnly = true)
public class CurrencyService {

    private final CurrencyRepository repository;

    public CurrencyService(CurrencyRepository repository) {
        this.repository = repository;
    }

    public List<Currency> findAll() {
        return repository.findAll();
    }

    public Currency getByCode(String code) {
        return repository.findById(code)
                .orElseThrow(() -> NotFoundException.of("Currency", code));
    }

    public boolean exists(String code) {
        return repository.existsById(code);
    }
}
