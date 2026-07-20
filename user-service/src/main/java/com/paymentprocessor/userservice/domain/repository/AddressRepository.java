package com.paymentprocessor.userservice.domain.repository;

import com.paymentprocessor.userservice.domain.address.Address;
import com.paymentprocessor.userservice.domain.address.AddressOwner;
import com.paymentprocessor.userservice.domain.address.AddressType;
import com.paymentprocessor.userservice.domain.valueobject.AddressId;

import java.util.List;
import java.util.Optional;

/**
 * Domain port for address persistence. Reads are owner-scoped. Speaks only in
 * domain types (rule 1).
 */
public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findByIdAndOwner(AddressId id, AddressOwner owner);

    List<Address> findByOwner(AddressOwner owner);

    /** Current default of a given type for an owner (if any), live rows only. */
    Optional<Address> findDefault(AddressOwner owner, AddressType type);
}
