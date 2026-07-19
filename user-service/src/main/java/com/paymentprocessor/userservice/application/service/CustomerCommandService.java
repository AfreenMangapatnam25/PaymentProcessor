package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.command.CreateCustomerCommand;
import com.paymentprocessor.userservice.application.command.DeleteCustomerCommand;
import com.paymentprocessor.userservice.application.command.EraseCustomerCommand;
import com.paymentprocessor.userservice.application.command.UpdateCustomerCommand;
import com.paymentprocessor.userservice.application.port.out.CryptoShredderPort;
import com.paymentprocessor.userservice.application.port.out.OutboxPort;
import com.paymentprocessor.userservice.application.validator.CustomerValidator;
import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.id.IdGenerator;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.domain.customer.Customer;
import com.paymentprocessor.userservice.domain.customer.CustomerMetadata;
import com.paymentprocessor.userservice.domain.event.CustomerCreatedEvent;
import com.paymentprocessor.userservice.domain.event.CustomerDeletedEvent;
import com.paymentprocessor.userservice.domain.event.CustomerErasedEvent;
import com.paymentprocessor.userservice.domain.event.CustomerUpdatedEvent;
import com.paymentprocessor.userservice.domain.exception.CustomerNotFoundException;
import com.paymentprocessor.userservice.domain.repository.CustomerRepository;
import com.paymentprocessor.userservice.domain.valueobject.CustomerId;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.MerchantId;
import com.paymentprocessor.userservice.domain.valueobject.PhoneNumber;
import com.paymentprocessor.userservice.domain.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Write use-cases for customers. Every operation is merchant-scoped and single-
 * transactional (state change + outbox event commit atomically). Business rules
 * stay in the {@link Customer} aggregate.
 */
@Service
public class CustomerCommandService {

    private static final String SUBJECT_TYPE = "CUSTOMER";

    private final CustomerRepository customerRepository;
    private final CustomerValidator customerValidator;
    private final CryptoShredderPort cryptoShredder;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final OutboxPort outbox;

    public CustomerCommandService(CustomerRepository customerRepository,
                                  CustomerValidator customerValidator,
                                  CryptoShredderPort cryptoShredder,
                                  IdGenerator idGenerator,
                                  ClockProvider clock,
                                  OutboxPort outbox) {
        this.customerRepository = customerRepository;
        this.customerValidator = customerValidator;
        this.cryptoShredder = cryptoShredder;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.outbox = outbox;
    }

    @Transactional
    public Customer create(CreateCustomerCommand cmd) {
        MerchantId merchantId = MerchantId.of(cmd.merchantId());
        Email email = Email.of(cmd.email());
        customerValidator.validateNewCustomer(merchantId, cmd.externalRef(), email);

        PhoneNumber phone = toPhone(cmd.phone());
        UserId userId = toUserId(cmd.userId());
        CustomerMetadata metadata = CustomerMetadata.of(cmd.metadata());
        CustomerId id = CustomerId.of(idGenerator.generateCustomerId());

        Customer customer = Customer.register(id, merchantId, email, cmd.fullName(),
                phone, cmd.externalRef(), userId, metadata, clock.now());
        Customer saved = customerRepository.save(customer);
        outbox.append(new CustomerCreatedEvent(
                saved.getId().value(), merchantId.value(), saved.getStatus().name(), clock.now()));
        return saved;
    }

    @Transactional
    public Customer update(UpdateCustomerCommand cmd) {
        MerchantId merchantId = MerchantId.of(cmd.merchantId());
        Customer customer = loadLive(cmd.customerId(), merchantId, cmd.expectedVersion());

        Email email = Email.of(cmd.email());
        boolean emailChanged = customer.getEmail() == null
                || !email.value().equals(customer.getEmail().value());
        if (emailChanged) {
            customerValidator.validateEmailAvailable(merchantId, email);
        }

        customer.updateContact(email, cmd.fullName(), toPhone(cmd.phone()));
        customer.updateMetadata(CustomerMetadata.of(cmd.metadata()));

        Customer saved = customerRepository.save(customer);
        outbox.append(new CustomerUpdatedEvent(
                saved.getId().value(), merchantId.value(), saved.getStatus().name(), clock.now()));
        return saved;
    }

    @Transactional
    public Customer delete(DeleteCustomerCommand cmd) {
        MerchantId merchantId = MerchantId.of(cmd.merchantId());
        Customer customer = loadLive(cmd.customerId(), merchantId, cmd.expectedVersion());

        Instant now = clock.now();
        customer.softDelete(now);
        Customer saved = customerRepository.save(customer);
        outbox.append(new CustomerDeletedEvent(saved.getId().value(), merchantId.value(), now, now));
        return saved;
    }

    @Transactional
    public Customer erase(EraseCustomerCommand cmd) {
        MerchantId merchantId = MerchantId.of(cmd.merchantId());
        Customer customer = customerRepository
                .findAnyByIdAndMerchant(CustomerId.of(cmd.customerId()), merchantId)
                .orElseThrow(() -> new CustomerNotFoundException(cmd.customerId()));
        ensureVersion(customer, cmd.expectedVersion());

        Instant now = clock.now();
        customer.erase(now);
        Customer saved = customerRepository.save(customer);
        cryptoShredder.shred(SUBJECT_TYPE, cmd.customerId());
        outbox.append(new CustomerErasedEvent(saved.getId().value(), merchantId.value(), now, now));
        return saved;
    }

    private Customer loadLive(String customerId, MerchantId merchantId, long expectedVersion) {
        Customer customer = customerRepository
                .findByIdAndMerchant(CustomerId.of(customerId), merchantId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        ensureVersion(customer, expectedVersion);
        return customer;
    }

    private void ensureVersion(Customer customer, long expectedVersion) {
        if (customer.getVersion() != expectedVersion) {
            throw new ConflictException(ErrorCode.CONFLICT,
                    "Customer was modified concurrently; reload and retry");
        }
    }

    private PhoneNumber toPhone(String phone) {
        return (phone != null && !phone.isBlank()) ? PhoneNumber.of(phone) : null;
    }

    private UserId toUserId(String userId) {
        return (userId != null && !userId.isBlank()) ? UserId.of(userId) : null;
    }
}
