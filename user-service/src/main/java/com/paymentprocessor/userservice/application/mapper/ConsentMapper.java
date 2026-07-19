package com.paymentprocessor.userservice.application.mapper;

import com.paymentprocessor.userservice.api.response.ConsentResponse;
import com.paymentprocessor.userservice.domain.consent.Consent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the Consent aggregate to its response DTO.
 */
@Mapper(componentModel = "spring")
public interface ConsentMapper {

    @Mapping(target = "id", expression = "java(consent.getId().value())")
    @Mapping(target = "subjectType", expression = "java(consent.getSubject().type().name())")
    @Mapping(target = "subjectId", expression = "java(consent.getSubject().id())")
    @Mapping(target = "kind", expression = "java(consent.getKind().name())")
    @Mapping(target = "granted", expression = "java(consent.isGranted())")
    @Mapping(target = "source", source = "source")
    @Mapping(target = "policyVersion", source = "policyVersion")
    @Mapping(target = "grantedAt", source = "grantedAt")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    ConsentResponse toResponse(Consent consent);
}
