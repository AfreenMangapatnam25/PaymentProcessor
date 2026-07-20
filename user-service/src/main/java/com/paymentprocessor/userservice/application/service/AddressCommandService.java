package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.command.CreateAddressCommand;
import com.paymentprocessor.userservice.application.command.DeleteAddressCommand;
import com.paymentprocessor.userservice.application.command.UpdateAddressCommand;
import com.paymentprocessor.userservice.application.port.out.OutboxPort;
import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.id.IdGenerator;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.domain.address.Address;
import com.paymentprocessor.userservice.domain.address.AddressOwner;
import com.paymentprocessor.userservice.domain.address.AddressType;
import com.paymentprocessor.userservice.domain.event.AddressCreatedEvent;
import com.paymentprocessor.userservice.domain.event.AddressDeletedEvent;
import com.paymentprocessor.userservice.domain.event.AddressUpdatedEvent;
import com.paymentprocessor.userservice.domain.exception.AddressNotFoundException;
import com.paymentprocessor.userservice.domain.repository.AddressRepository;
import com.paymentprocessor.userservice.domain.valueobject.AddressId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Write use-cases for addresses. Enforces the "one default per owner per type"
 * rule by demoting the current default before promoting a new one, so the
 * database's partial unique index is never violated. Owner-scoped and single-
 * transactional (state change + outbox event commit atomically).
 */
@Service
public class AddressCommandService {

    private final AddressRepository addressRepository;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final OutboxPort outbox;

    public AddressCommandService(AddressRepository addressRepository,
                                 IdGenerator idGenerator,
                                 ClockProvider clock,
                                 OutboxPort outbox) {
        this.addressRepository = addressRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.outbox = outbox;
    }

    @Transactional
    public Address create(CreateAddressCommand cmd) {
        AddressOwner owner = AddressOwner.of(cmd.ownerType(), cmd.ownerId());
        AddressType type = cmd.addressType() != null ? cmd.addressType() : AddressType.SHIPPING;
        AddressId id = AddressId.of(idGenerator.generateAddressId());

        if (cmd.defaultAddress()) {
            demoteCurrentDefault(owner, type, null);
        }

        Address address = Address.register(id, owner, type, cmd.line1(), cmd.line2(),
                cmd.city(), cmd.region(), cmd.postalCode(), cmd.countryCode(),
                cmd.defaultAddress(), clock.now());
        Address saved = addressRepository.save(address);
        outbox.append(new AddressCreatedEvent(saved.getId().value(), owner.type().name(),
                owner.id(), type.name(), saved.getCountryCode(), clock.now()));
        return saved;
    }

    @Transactional
    public Address update(UpdateAddressCommand cmd) {
        AddressOwner owner = AddressOwner.of(cmd.ownerType(), cmd.ownerId());
        Address address = load(cmd.addressId(), owner, cmd.expectedVersion());

        AddressType type = cmd.addressType() != null ? cmd.addressType() : address.getAddressType();
        address.update(type, cmd.line1(), cmd.line2(), cmd.city(), cmd.region(),
                cmd.postalCode(), cmd.countryCode());

        if (cmd.defaultAddress()) {
            demoteCurrentDefault(owner, type, address.getId().value());
            address.markDefault();
        } else {
            address.clearDefault();
        }

        Address saved = addressRepository.save(address);
        outbox.append(new AddressUpdatedEvent(saved.getId().value(), owner.type().name(),
                owner.id(), type.name(), saved.getCountryCode(), clock.now()));
        return saved;
    }

    @Transactional
    public Address delete(DeleteAddressCommand cmd) {
        AddressOwner owner = AddressOwner.of(cmd.ownerType(), cmd.ownerId());
        Address address = load(cmd.addressId(), owner, cmd.expectedVersion());

        Instant now = clock.now();
        address.softDelete(now);
        Address saved = addressRepository.save(address);
        outbox.append(new AddressDeletedEvent(saved.getId().value(), owner.type().name(),
                owner.id(), now, now));
        return saved;
    }

    private Address load(String addressId, AddressOwner owner, long expectedVersion) {
        Address address = addressRepository.findByIdAndOwner(AddressId.of(addressId), owner)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
        if (address.getVersion() != expectedVersion) {
            throw new ConflictException(ErrorCode.CONFLICT,
                    "Address was modified concurrently; reload and retry");
        }
        return address;
    }

    private void demoteCurrentDefault(AddressOwner owner, AddressType type, String excludeAddressId) {
        Optional<Address> current = addressRepository.findDefault(owner, type);
        if (current.isPresent() && !current.get().getId().value().equals(excludeAddressId)) {
            Address existing = current.get();
            existing.clearDefault();
            addressRepository.save(existing);
        }
    }
}
