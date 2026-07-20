package com.paymentprocessor.userservice.application.mapper;

import com.paymentprocessor.userservice.api.response.CustomerResponse;
import com.paymentprocessor.userservice.domain.customer.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the Customer aggregate to its response DTO. Value objects are unwrapped
 * via expressions; the opaque instrument token passes through verbatim.
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", expression = "java(customer.getId().value())")
    @Mapping(target = "merchantId", expression = "java(customer.getMerchantId().value())")
    @Mapping(target = "userId", expression = "java(customer.getUserId() != null ? customer.getUserId().value() : null)")
    @Mapping(target = "externalRef", source = "externalRef")
    @Mapping(target = "email", expression = "java(customer.getEmail() != null ? customer.getEmail().value() : null)")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phone", expression = "java(customer.getPhone() != null ? customer.getPhone().value() : null)")
    @Mapping(target = "defaultInstrumentToken", source = "defaultInstrumentToken")
    @Mapping(target = "status", expression = "java(customer.getStatus().name())")
    @Mapping(target = "metadata", expression = "java(customer.getMetadata().values())")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "deletedAt", source = "deletedAt")
    @Mapping(target = "erasedAt", source = "erasedAt")
    CustomerResponse toResponse(Customer customer);
}
