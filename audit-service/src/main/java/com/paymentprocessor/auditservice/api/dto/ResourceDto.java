package com.paymentprocessor.auditservice.api.dto;

import com.paymentprocessor.auditservice.domain.ResourceRef;

import jakarta.validation.constraints.NotBlank;

public record ResourceDto(
        @NotBlank String type,
        String id) {

    public ResourceRef toDomain() {
        return new ResourceRef(type, id);
    }
}
