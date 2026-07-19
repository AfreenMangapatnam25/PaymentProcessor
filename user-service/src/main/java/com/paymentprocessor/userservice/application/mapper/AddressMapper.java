package com.paymentprocessor.userservice.application.mapper;

import com.paymentprocessor.userservice.api.response.AddressResponse;
import com.paymentprocessor.userservice.domain.address.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the Address aggregate to its response DTO.
 */
@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "id", expression = "java(address.getId().value())")
    @Mapping(target = "ownerType", expression = "java(address.getOwner().type().name())")
    @Mapping(target = "ownerId", expression = "java(address.getOwner().id())")
    @Mapping(target = "addressType", expression = "java(address.getAddressType().name())")
    @Mapping(target = "line1", source = "line1")
    @Mapping(target = "line2", source = "line2")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "region", source = "region")
    @Mapping(target = "postalCode", source = "postalCode")
    @Mapping(target = "countryCode", source = "countryCode")
    @Mapping(target = "defaultAddress", expression = "java(address.isDefaultAddress())")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "deletedAt", source = "deletedAt")
    AddressResponse toResponse(Address address);
}
