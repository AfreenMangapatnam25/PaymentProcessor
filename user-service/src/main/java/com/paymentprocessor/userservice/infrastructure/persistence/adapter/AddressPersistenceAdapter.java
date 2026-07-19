package com.paymentprocessor.userservice.infrastructure.persistence.adapter;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.exception.InfrastructureException;
import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.domain.address.Address;
import com.paymentprocessor.userservice.domain.address.AddressOwner;
import com.paymentprocessor.userservice.domain.address.AddressType;
import com.paymentprocessor.userservice.domain.address.OwnerType;
import com.paymentprocessor.userservice.domain.repository.AddressRepository;
import com.paymentprocessor.userservice.domain.valueobject.AddressId;
import com.paymentprocessor.userservice.infrastructure.encryption.DataKeyService;
import com.paymentprocessor.userservice.infrastructure.encryption.EncryptionService;
import com.paymentprocessor.userservice.infrastructure.encryption.IssuedDataKey;
import com.paymentprocessor.userservice.infrastructure.persistence.entity.AddressEntity;
import com.paymentprocessor.userservice.infrastructure.persistence.repository.AddressJpaRepository;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing the {@link AddressRepository} port. Encrypts the address
 * lines with the OWNER's DEK (looked up by subject), so an address is
 * cryptographically tied to its owner: erasing the owner also renders the
 * address lines unrecoverable. The domain never sees an entity or ciphertext.
 */
@Component
public class AddressPersistenceAdapter implements AddressRepository {

    private final AddressJpaRepository addressJpa;
    private final DataKeyService dataKeyService;
    private final EncryptionService encryption;
    private final ClockProvider clock;

    public AddressPersistenceAdapter(AddressJpaRepository addressJpa,
                                     DataKeyService dataKeyService,
                                     EncryptionService encryption,
                                     ClockProvider clock) {
        this.addressJpa = addressJpa;
        this.dataKeyService = dataKeyService;
        this.encryption = encryption;
        this.clock = clock;
    }

    @Override
    public Address save(Address address) {
        return addressJpa.findById(address.getId().value())
                .map(existing -> update(existing, address))
                .orElseGet(() -> insert(address));
    }

    private Address insert(Address address) {
        AddressOwner owner = address.getOwner();
        IssuedDataKey ownerKey = dataKeyService
                .loadActiveKeyForSubject(owner.type().name(), owner.id())
                .orElseThrow(() -> new ValidationException("owner",
                        "Owner not found or not eligible to hold addresses"));

        AddressEntity entity = new AddressEntity();
        entity.setId(address.getId().value());
        entity.setOwnerType(owner.type().name());
        entity.setOwnerId(owner.id());
        entity.setAddressType(address.getAddressType().name());
        entity.setCountryCode(address.getCountryCode());
        entity.setDefault(address.isDefaultAddress());
        entity.setCryptoKeyId(ownerKey.cryptoKeyId());
        entity.setDeletedAt(address.getDeletedAt());
        entity.setCreatedAt(address.getCreatedAt() != null ? address.getCreatedAt() : clock.now());
        entity.setUpdatedAt(clock.now());
        writeLines(entity, address, ownerKey.dek());

        AddressEntity saved = addressJpa.save(entity);
        return address.toBuilder().version(saved.getVersion()).build();
    }

    private Address update(AddressEntity entity, Address address) {
        if (address.getVersion() != entity.getVersion()) {
            throw new ConflictException(ErrorCode.CONFLICT,
                    "Address was modified concurrently; reload and retry");
        }
        SecretKey dek = dataKeyService.loadActiveKey(entity.getCryptoKeyId())
                .orElseThrow(() -> new InfrastructureException(
                        ErrorCode.ENCRYPTION_ERROR, "Active data key unavailable for address owner"));

        entity.setAddressType(address.getAddressType().name());
        entity.setCountryCode(address.getCountryCode());
        entity.setDefault(address.isDefaultAddress());
        entity.setDeletedAt(address.getDeletedAt());
        writeLines(entity, address, dek);

        AddressEntity saved = addressJpa.saveAndFlush(entity);
        return address.toBuilder().version(saved.getVersion()).build();
    }

    @Override
    public Optional<Address> findByIdAndOwner(AddressId id, AddressOwner owner) {
        return addressJpa.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                        id.value(), owner.type().name(), owner.id())
                .map(this::toDomain);
    }

    @Override
    public List<Address> findByOwner(AddressOwner owner) {
        return addressJpa.findByOwnerTypeAndOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        owner.type().name(), owner.id())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Address> findDefault(AddressOwner owner, AddressType type) {
        return addressJpa.findByOwnerTypeAndOwnerIdAndAddressTypeAndIsDefaultTrueAndDeletedAtIsNull(
                        owner.type().name(), owner.id(), type.name())
                .map(this::toDomain);
    }

    // ----- mapping helpers -------------------------------------------------

    private void writeLines(AddressEntity entity, Address address, SecretKey dek) {
        entity.setLine1Encrypted(encryption.encryptString(address.getLine1(), dek));
        entity.setLine2Encrypted(encryption.encryptString(address.getLine2(), dek));
        entity.setCityEncrypted(encryption.encryptString(address.getCity(), dek));
        entity.setRegionEncrypted(encryption.encryptString(address.getRegion(), dek));
        entity.setPostalCodeEncrypted(encryption.encryptString(address.getPostalCode(), dek));
    }

    private Address toDomain(AddressEntity entity) {
        Optional<SecretKey> dek = dataKeyService.loadActiveKey(entity.getCryptoKeyId());
        String line1 = null, line2 = null, city = null, region = null, postalCode = null;
        if (dek.isPresent()) {
            SecretKey key = dek.get();
            line1 = encryption.decryptToString(entity.getLine1Encrypted(), key);
            line2 = encryption.decryptToString(entity.getLine2Encrypted(), key);
            city = encryption.decryptToString(entity.getCityEncrypted(), key);
            region = encryption.decryptToString(entity.getRegionEncrypted(), key);
            postalCode = encryption.decryptToString(entity.getPostalCodeEncrypted(), key);
        }

        return Address.builder()
                .id(AddressId.of(entity.getId()))
                .owner(AddressOwner.of(OwnerType.valueOf(entity.getOwnerType()), entity.getOwnerId()))
                .addressType(AddressType.valueOf(entity.getAddressType()))
                .line1(line1)
                .line2(line2)
                .city(city)
                .region(region)
                .postalCode(postalCode)
                .countryCode(entity.getCountryCode())
                .defaultAddress(entity.isDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .version(entity.getVersion())
                .build();
    }
}
