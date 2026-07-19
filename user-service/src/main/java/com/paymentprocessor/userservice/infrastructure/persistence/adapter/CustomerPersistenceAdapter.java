package com.paymentprocessor.userservice.infrastructure.persistence.adapter;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.exception.InfrastructureException;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.common.util.JsonUtils;
import com.paymentprocessor.userservice.domain.customer.Customer;
import com.paymentprocessor.userservice.domain.customer.CustomerMetadata;
import com.paymentprocessor.userservice.domain.customer.CustomerStatus;
import com.paymentprocessor.userservice.domain.repository.CustomerRepository;
import com.paymentprocessor.userservice.domain.valueobject.CustomerId;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.MerchantId;
import com.paymentprocessor.userservice.domain.valueobject.PhoneNumber;
import com.paymentprocessor.userservice.domain.valueobject.UserId;
import com.paymentprocessor.userservice.infrastructure.encryption.BlindIndexService;
import com.paymentprocessor.userservice.infrastructure.encryption.DataKeyService;
import com.paymentprocessor.userservice.infrastructure.encryption.EncryptionService;
import com.paymentprocessor.userservice.infrastructure.encryption.IssuedDataKey;
import com.paymentprocessor.userservice.infrastructure.persistence.entity.CustomerEntity;
import com.paymentprocessor.userservice.infrastructure.persistence.repository.CustomerJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter implementing the {@link CustomerRepository} port. Maps the aggregate
 * to the {@code customers} table with envelope encryption of PII and blind
 * indexing, and translates soft-delete/erasure state. The domain never sees an
 * entity or ciphertext.
 */
@Component
public class CustomerPersistenceAdapter implements CustomerRepository {

    private static final String SUBJECT_TYPE = "CUSTOMER";

    private final CustomerJpaRepository customerJpa;
    private final DataKeyService dataKeyService;
    private final EncryptionService encryption;
    private final BlindIndexService blindIndex;
    private final ClockProvider clock;

    public CustomerPersistenceAdapter(CustomerJpaRepository customerJpa,
                                      DataKeyService dataKeyService,
                                      EncryptionService encryption,
                                      BlindIndexService blindIndex,
                                      ClockProvider clock) {
        this.customerJpa = customerJpa;
        this.dataKeyService = dataKeyService;
        this.encryption = encryption;
        this.blindIndex = blindIndex;
        this.clock = clock;
    }

    @Override
    public Customer save(Customer customer) {
        String id = customer.getId().value();
        return customerJpa.findById(id)
                .map(existing -> update(existing, customer))
                .orElseGet(() -> insert(customer));
    }

    private Customer insert(Customer customer) {
        String id = customer.getId().value();
        IssuedDataKey issued = dataKeyService.issueFor(SUBJECT_TYPE, id);

        CustomerEntity entity = new CustomerEntity();
        entity.setId(id);
        entity.setMerchantId(customer.getMerchantId().value());
        entity.setUserId(customer.getUserId() != null ? customer.getUserId().value() : null);
        entity.setExternalRef(customer.getExternalRef());
        entity.setCryptoKeyId(issued.cryptoKeyId());
        entity.setStatus(customer.getStatus().name());
        entity.setDefaultInstrumentToken(customer.getDefaultInstrumentToken());
        entity.setDeletedAt(customer.getDeletedAt());
        entity.setErasedAt(customer.getErasedAt());
        entity.setCreatedAt(customer.getCreatedAt() != null ? customer.getCreatedAt() : clock.now());
        entity.setUpdatedAt(clock.now());
        writePii(entity, customer, issued.dek());
        entity.setMetadata(JsonUtils.toJson(customer.getMetadata().values()));

        CustomerEntity saved = customerJpa.save(entity);
        return customer.toBuilder().version(saved.getVersion()).build();
    }

    private Customer update(CustomerEntity entity, Customer customer) {
        if (customer.getVersion() != entity.getVersion()) {
            throw new ConflictException(ErrorCode.CONFLICT,
                    "Customer was modified concurrently; reload and retry");
        }
        entity.setStatus(customer.getStatus().name());
        entity.setUserId(customer.getUserId() != null ? customer.getUserId().value() : null);
        entity.setDefaultInstrumentToken(customer.getDefaultInstrumentToken());
        entity.setDeletedAt(customer.getDeletedAt());
        entity.setErasedAt(customer.getErasedAt());
        entity.setMetadata(JsonUtils.toJson(customer.getMetadata().values()));

        if (customer.isErased()) {
            clearPii(entity);
        } else {
            SecretKey dek = dataKeyService.loadActiveKey(entity.getCryptoKeyId())
                    .orElseThrow(() -> new InfrastructureException(
                            ErrorCode.ENCRYPTION_ERROR, "Active data key unavailable for customer"));
            writePii(entity, customer, dek);
        }
        CustomerEntity saved = customerJpa.saveAndFlush(entity);
        return customer.toBuilder().version(saved.getVersion()).build();
    }

    @Override
    public Optional<Customer> findByIdAndMerchant(CustomerId id, MerchantId merchantId) {
        return customerJpa.findByIdAndMerchantIdAndDeletedAtIsNull(id.value(), merchantId.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<Customer> findAnyByIdAndMerchant(CustomerId id, MerchantId merchantId) {
        return customerJpa.findByIdAndMerchantId(id.value(), merchantId.value()).map(this::toDomain);
    }

    @Override
    public List<Customer> findByMerchant(MerchantId merchantId, int page, int size) {
        return customerJpa.findByMerchantIdAndDeletedAtIsNull(merchantId.value(), PageRequest.of(page, size))
                .map(this::toDomain)
                .getContent();
    }

    @Override
    public long countByMerchant(MerchantId merchantId) {
        return customerJpa.countByMerchantIdAndDeletedAtIsNull(merchantId.value());
    }

    @Override
    public boolean existsByMerchantAndExternalRef(MerchantId merchantId, String externalRef) {
        return externalRef != null
                && customerJpa.existsByMerchantIdAndExternalRefAndDeletedAtIsNull(merchantId.value(), externalRef);
    }

    @Override
    public boolean existsByMerchantAndEmail(MerchantId merchantId, Email email) {
        return customerJpa.existsByMerchantIdAndEmailIndexAndDeletedAtIsNull(
                merchantId.value(), blindIndex.index(email.value()));
    }

    // ----- mapping helpers -------------------------------------------------

    private void writePii(CustomerEntity entity, Customer customer, SecretKey dek) {
        entity.setEmailEncrypted(encryption.encryptString(customer.getEmail().value(), dek));
        entity.setEmailIndex(blindIndex.index(customer.getEmail().value()));
        entity.setFullNameEncrypted(encryption.encryptString(customer.getFullName(), dek));
        if (customer.getPhone() != null) {
            entity.setPhoneEncrypted(encryption.encryptString(customer.getPhone().value(), dek));
            entity.setPhoneIndex(blindIndex.index(customer.getPhone().value()));
        } else {
            entity.setPhoneEncrypted(null);
            entity.setPhoneIndex(null);
        }
    }

    private void clearPii(CustomerEntity entity) {
        entity.setEmailEncrypted(null);
        entity.setEmailIndex(null);
        entity.setFullNameEncrypted(null);
        entity.setPhoneEncrypted(null);
        entity.setPhoneIndex(null);
        entity.setDefaultInstrumentToken(null);
    }

    @SuppressWarnings("unchecked")
    private Customer toDomain(CustomerEntity entity) {
        CustomerStatus status = CustomerStatus.valueOf(entity.getStatus());
        Email email = null;
        String fullName = null;
        PhoneNumber phone = null;

        if (status != CustomerStatus.ERASED && entity.getEmailEncrypted() != null) {
            Optional<SecretKey> dek = dataKeyService.loadActiveKey(entity.getCryptoKeyId());
            if (dek.isPresent()) {
                email = Email.of(encryption.decryptToString(entity.getEmailEncrypted(), dek.get()));
                fullName = encryption.decryptToString(entity.getFullNameEncrypted(), dek.get());
                if (entity.getPhoneEncrypted() != null) {
                    phone = PhoneNumber.of(encryption.decryptToString(entity.getPhoneEncrypted(), dek.get()));
                }
            }
        }

        CustomerMetadata metadata = entity.getMetadata() != null
                ? CustomerMetadata.of(JsonUtils.fromJson(entity.getMetadata(), Map.class))
                : CustomerMetadata.empty();

        return Customer.builder()
                .id(CustomerId.of(entity.getId()))
                .merchantId(MerchantId.of(entity.getMerchantId()))
                .userId(entity.getUserId() != null ? UserId.of(entity.getUserId()) : null)
                .externalRef(entity.getExternalRef())
                .email(email)
                .fullName(fullName)
                .phone(phone)
                .defaultInstrumentToken(entity.getDefaultInstrumentToken())
                .status(status)
                .metadata(metadata)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .erasedAt(entity.getErasedAt())
                .version(entity.getVersion())
                .build();
    }
}
