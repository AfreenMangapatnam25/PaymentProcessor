package com.paymentprocessor.userservice.application.mapper;

import com.paymentprocessor.userservice.api.response.ConsentResponse;
import com.paymentprocessor.userservice.domain.consent.Consent;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-25T22:52:58+0530",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.jar, environment: Java 21.0.11 (Oracle Corporation)"
)
@Component
public class ConsentMapperImpl implements ConsentMapper {

    @Override
    public ConsentResponse toResponse(Consent consent) {
        if ( consent == null ) {
            return null;
        }

        String source = null;
        String policyVersion = null;
        Instant grantedAt = null;
        Instant revokedAt = null;
        long version = 0L;
        Instant createdAt = null;
        Instant updatedAt = null;

        source = consent.getSource();
        policyVersion = consent.getPolicyVersion();
        grantedAt = consent.getGrantedAt();
        revokedAt = consent.getRevokedAt();
        version = consent.getVersion();
        createdAt = consent.getCreatedAt();
        updatedAt = consent.getUpdatedAt();

        String id = consent.getId().value();
        String subjectType = consent.getSubject().type().name();
        String subjectId = consent.getSubject().id();
        String kind = consent.getKind().name();
        boolean granted = consent.isGranted();

        ConsentResponse consentResponse = new ConsentResponse( id, subjectType, subjectId, kind, granted, source, policyVersion, grantedAt, revokedAt, version, createdAt, updatedAt );

        return consentResponse;
    }
}
