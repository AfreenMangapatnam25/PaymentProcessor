package com.paymentprocessor.userservice.application.mapper;

import com.paymentprocessor.userservice.api.response.AddressResponse;
import com.paymentprocessor.userservice.domain.address.Address;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-19T23:25:23+0530",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.jar, environment: Java 21.0.11 (Oracle Corporation)"
)
@Component
public class AddressMapperImpl implements AddressMapper {

    @Override
    public AddressResponse toResponse(Address address) {
        if ( address == null ) {
            return null;
        }

        String line1 = null;
        String line2 = null;
        String city = null;
        String region = null;
        String postalCode = null;
        String countryCode = null;
        long version = 0L;
        Instant createdAt = null;
        Instant updatedAt = null;
        Instant deletedAt = null;

        line1 = address.getLine1();
        line2 = address.getLine2();
        city = address.getCity();
        region = address.getRegion();
        postalCode = address.getPostalCode();
        countryCode = address.getCountryCode();
        version = address.getVersion();
        createdAt = address.getCreatedAt();
        updatedAt = address.getUpdatedAt();
        deletedAt = address.getDeletedAt();

        String id = address.getId().value();
        String ownerType = address.getOwner().type().name();
        String ownerId = address.getOwner().id();
        String addressType = address.getAddressType().name();
        boolean defaultAddress = address.isDefaultAddress();

        AddressResponse addressResponse = new AddressResponse( id, ownerType, ownerId, addressType, line1, line2, city, region, postalCode, countryCode, defaultAddress, version, createdAt, updatedAt, deletedAt );

        return addressResponse;
    }
}
