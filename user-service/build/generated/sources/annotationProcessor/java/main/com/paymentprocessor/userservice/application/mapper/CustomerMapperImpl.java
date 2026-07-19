package com.paymentprocessor.userservice.application.mapper;

import com.paymentprocessor.userservice.api.response.CustomerResponse;
import com.paymentprocessor.userservice.domain.customer.Customer;
import java.time.Instant;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-19T23:25:24+0530",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.jar, environment: Java 21.0.11 (Oracle Corporation)"
)
@Component
public class CustomerMapperImpl implements CustomerMapper {

    @Override
    public CustomerResponse toResponse(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        String externalRef = null;
        String fullName = null;
        String defaultInstrumentToken = null;
        long version = 0L;
        Instant createdAt = null;
        Instant updatedAt = null;
        Instant deletedAt = null;
        Instant erasedAt = null;

        externalRef = customer.getExternalRef();
        fullName = customer.getFullName();
        defaultInstrumentToken = customer.getDefaultInstrumentToken();
        version = customer.getVersion();
        createdAt = customer.getCreatedAt();
        updatedAt = customer.getUpdatedAt();
        deletedAt = customer.getDeletedAt();
        erasedAt = customer.getErasedAt();

        String id = customer.getId().value();
        String merchantId = customer.getMerchantId().value();
        String userId = customer.getUserId() != null ? customer.getUserId().value() : null;
        String email = customer.getEmail() != null ? customer.getEmail().value() : null;
        String phone = customer.getPhone() != null ? customer.getPhone().value() : null;
        String status = customer.getStatus().name();
        Map<String, String> metadata = customer.getMetadata().values();

        CustomerResponse customerResponse = new CustomerResponse( id, merchantId, userId, externalRef, email, fullName, phone, defaultInstrumentToken, status, metadata, version, createdAt, updatedAt, deletedAt, erasedAt );

        return customerResponse;
    }
}
