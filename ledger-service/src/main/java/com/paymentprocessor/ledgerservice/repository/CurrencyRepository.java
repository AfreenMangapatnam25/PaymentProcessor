package com.paymentprocessor.ledgerservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.paymentprocessor.ledgerservice.entity.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {
}
