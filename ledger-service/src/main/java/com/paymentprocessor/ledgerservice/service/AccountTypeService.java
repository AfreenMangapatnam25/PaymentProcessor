package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.entity.AccountType;
import com.paymentprocessor.ledgerservice.repository.AccountTypeRepository;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to the reference account types (classification + normal balance).
 */
@Service
@Transactional(readOnly = true)
public class AccountTypeService {

    private final AccountTypeRepository repository;

    public AccountTypeService(AccountTypeRepository repository) {
        this.repository = repository;
    }

    public List<AccountType> findAll() {
        return repository.findAll();
    }

    public AccountType getByCode(String code) {
        return repository.findById(code)
                .orElseThrow(() -> NotFoundException.of("Account type", code));
    }
}
