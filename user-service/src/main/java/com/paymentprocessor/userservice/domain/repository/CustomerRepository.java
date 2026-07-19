package com.paymentprocessor.userservice.domain.repository;

import com.paymentprocessor.userservice.domain.customer.Customer;
import com.paymentprocessor.userservice.domain.valueobject.CustomerId;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.MerchantId;

import java.util.List;
import java.util.Optional;

/**
 * Domain port for customer persistence. Every read is merchant-scoped so tenant
 * isolation is structurally enforced, not merely conventional (rule 12). Speaks
 * only in domain types (rule 1).
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    /** Fetch a live (not soft-deleted) customer within the merchant's scope. */
    Optional<Customer> findByIdAndMerchant(CustomerId id, MerchantId merchantId);

    /** Include soft-deleted rows -- used by erasure/audit flows. */
    Optional<Customer> findAnyByIdAndMerchant(CustomerId id, MerchantId merchantId);

    List<Customer> findByMerchant(MerchantId merchantId, int page, int size);

    long countByMerchant(MerchantId merchantId);

    boolean existsByMerchantAndExternalRef(MerchantId merchantId, String externalRef);

    boolean existsByMerchantAndEmail(MerchantId merchantId, Email email);
}
