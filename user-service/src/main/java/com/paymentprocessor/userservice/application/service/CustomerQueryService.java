package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.query.GetCustomerQuery;
import com.paymentprocessor.userservice.application.query.SearchCustomerQuery;
import com.paymentprocessor.userservice.domain.customer.Customer;
import com.paymentprocessor.userservice.domain.exception.CustomerNotFoundException;
import com.paymentprocessor.userservice.domain.repository.CustomerRepository;
import com.paymentprocessor.userservice.domain.valueobject.CustomerId;
import com.paymentprocessor.userservice.domain.valueobject.MerchantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read use-cases for customers, always merchant-scoped.
 */
@Service
@Transactional(readOnly = true)
public class CustomerQueryService {

    private final CustomerRepository customerRepository;

    public CustomerQueryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer getById(GetCustomerQuery query) {
        return customerRepository
                .findByIdAndMerchant(CustomerId.of(query.customerId()), MerchantId.of(query.merchantId()))
                .orElseThrow(() -> new CustomerNotFoundException(query.customerId()));
    }

    public List<Customer> search(SearchCustomerQuery query) {
        return customerRepository.findByMerchant(MerchantId.of(query.merchantId()), query.page(), query.size());
    }

    public long count(String merchantId) {
        return customerRepository.countByMerchant(MerchantId.of(merchantId));
    }
}
