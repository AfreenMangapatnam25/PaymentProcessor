package com.paymentprocessor.userservice.application.validator;

import com.paymentprocessor.userservice.domain.exception.DuplicateCustomerException;
import com.paymentprocessor.userservice.domain.repository.CustomerRepository;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.MerchantId;
import org.springframework.stereotype.Component;

/**
 * Per-merchant uniqueness checks for customers (external reference, email).
 * Scoped by merchant so the same email may exist under different merchants
 * (isolation, rule 12). Messages carry no PII (rule 13).
 */
@Component
public class CustomerValidator {

    private final CustomerRepository customerRepository;

    public CustomerValidator(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public void validateNewCustomer(MerchantId merchantId, String externalRef, Email email) {
        if (externalRef != null && !externalRef.isBlank()
                && customerRepository.existsByMerchantAndExternalRef(merchantId, externalRef)) {
            throw DuplicateCustomerException.externalRef();
        }
        if (customerRepository.existsByMerchantAndEmail(merchantId, email)) {
            throw DuplicateCustomerException.email();
        }
    }

    public void validateEmailAvailable(MerchantId merchantId, Email email) {
        if (customerRepository.existsByMerchantAndEmail(merchantId, email)) {
            throw DuplicateCustomerException.email();
        }
    }
}
