package com.paymentprocessor.userservice.application.service;

import com.paymentprocessor.userservice.application.query.GetAddressQuery;
import com.paymentprocessor.userservice.application.query.ListAddressesQuery;
import com.paymentprocessor.userservice.domain.address.Address;
import com.paymentprocessor.userservice.domain.address.AddressOwner;
import com.paymentprocessor.userservice.domain.exception.AddressNotFoundException;
import com.paymentprocessor.userservice.domain.repository.AddressRepository;
import com.paymentprocessor.userservice.domain.valueobject.AddressId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read use-cases for addresses, always owner-scoped.
 */
@Service
@Transactional(readOnly = true)
public class AddressQueryService {

    private final AddressRepository addressRepository;

    public AddressQueryService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public Address getById(GetAddressQuery query) {
        AddressOwner owner = AddressOwner.of(query.ownerType(), query.ownerId());
        return addressRepository.findByIdAndOwner(AddressId.of(query.addressId()), owner)
                .orElseThrow(() -> new AddressNotFoundException(query.addressId()));
    }

    public List<Address> list(ListAddressesQuery query) {
        return addressRepository.findByOwner(AddressOwner.of(query.ownerType(), query.ownerId()));
    }
}
