package com.paymentprocessor.userservice.domain.customer;

import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.domain.valueobject.CustomerId;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.MerchantId;
import com.paymentprocessor.userservice.domain.valueobject.PhoneNumber;
import com.paymentprocessor.userservice.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Customer aggregate root: a merchant-scoped buyer. Carries its own PII (email,
 * full name, optional phone), an opaque default instrument token (a
 * vault-service reference this service never dereferences), optional link to a
 * platform user, and free-form metadata.
 *
 * <p>Invariants live here (rule 4): a customer always belongs to exactly one
 * merchant and that never changes; deletion is soft (status DELETED, retained
 * for ledger integrity, rule 9); erasure is terminal and drops PII (rule 10);
 * an erased/deleted customer cannot be mutated. Encryption/soft-delete columns
 * are a persistence concern and absent from the domain.
 */
@Getter
@Builder(toBuilder = true)
public class Customer {

    private final CustomerId id;
    private final MerchantId merchantId;
    private UserId userId;                 // optional link to a platform user
    private final String externalRef;      // merchant's own id for this buyer
    private Email email;
    private String fullName;
    private PhoneNumber phone;             // optional
    private String defaultInstrumentToken; // opaque -> vault-service
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;
    @Builder.Default
    private CustomerMetadata metadata = CustomerMetadata.empty();
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private Instant erasedAt;
    @Builder.Default
    private long version = 0L;

    public static Customer register(CustomerId id,
                                    MerchantId merchantId,
                                    Email email,
                                    String fullName,
                                    PhoneNumber phone,
                                    String externalRef,
                                    UserId userId,
                                    CustomerMetadata metadata,
                                    Instant now) {
        return Customer.builder()
                .id(id)
                .merchantId(merchantId)
                .email(requireEmail(email))
                .fullName(requireName(fullName))
                .phone(phone)
                .externalRef(externalRef)
                .userId(userId)
                .metadata(metadata != null ? metadata : CustomerMetadata.empty())
                .status(CustomerStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    public void updateContact(Email email, String fullName, PhoneNumber phone) {
        assertMutable();
        this.email = requireEmail(email);
        this.fullName = requireName(fullName);
        this.phone = phone;
        touch();
    }

    public void updateMetadata(CustomerMetadata metadata) {
        assertMutable();
        this.metadata = metadata != null ? metadata : CustomerMetadata.empty();
        touch();
    }

    public void attachDefaultInstrument(String instrumentToken) {
        assertMutable();
        if (instrumentToken == null || instrumentToken.isBlank()) {
            throw new ValidationException("defaultInstrumentToken", "Instrument token must not be blank");
        }
        this.defaultInstrumentToken = instrumentToken;
        touch();
    }

    public void linkUser(UserId userId) {
        assertMutable();
        this.userId = userId;
        touch();
    }

    public void activate() {
        assertMutable();
        this.status = CustomerStatus.ACTIVE;
        touch();
    }

    public void deactivate() {
        assertMutable();
        this.status = CustomerStatus.INACTIVE;
        touch();
    }

    /** Soft delete (rule 9): retain the row, mark it deleted. */
    public void softDelete(Instant now) {
        if (status.isTerminal()) {
            throw new ValidationException("status", "Erased customers cannot be deleted");
        }
        this.status = CustomerStatus.DELETED;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    /** GDPR erasure (rule 10): drop PII, terminal state. DEK destroyed by infra. */
    public void erase(Instant now) {
        if (status.isTerminal()) {
            throw new ValidationException("status", "Customer already erased");
        }
        this.status = CustomerStatus.ERASED;
        this.email = null;
        this.fullName = null;
        this.phone = null;
        this.defaultInstrumentToken = null;
        this.metadata = CustomerMetadata.empty();
        this.erasedAt = now;
        if (this.deletedAt == null) {
            this.deletedAt = now;
        }
        this.updatedAt = now;
    }

    public boolean isErased() {
        return status == CustomerStatus.ERASED;
    }

    public boolean isDeleted() {
        return status.isDeleted();
    }

    private void assertMutable() {
        if (status.isDeleted()) {
            throw new ValidationException("status", "Deleted or erased customers are immutable");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static Email requireEmail(Email email) {
        if (email == null) {
            throw new ValidationException("email", "Email is required");
        }
        return email;
    }

    private static String requireName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("fullName", "Full name is required");
        }
        String trimmed = fullName.trim();
        if (trimmed.length() > 200) {
            throw new ValidationException("fullName", "Full name must not exceed 200 characters");
        }
        return trimmed;
    }
}
