package com.paymentprocessor.auditservice.api.dto;

import com.paymentprocessor.auditservice.domain.Actor;

import jakarta.validation.constraints.NotBlank;

public record ActorDto(
        @NotBlank String type,
        String id,
        String ip,
        String ua) {

    public Actor toDomain() {
        return new Actor(type, id, ip, ua);
    }
}
