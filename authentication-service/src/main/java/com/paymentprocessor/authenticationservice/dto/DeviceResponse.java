package com.paymentprocessor.authenticationservice.dto;

import com.paymentprocessor.authenticationservice.domain.DeviceTrust;
import com.paymentprocessor.authenticationservice.entity.Device;

import java.time.Instant;

public record DeviceResponse(
        String id,
        String label,
        String fingerprint,
        DeviceTrust trustLevel,
        String lastIp,
        Instant lastSeenAt,
        Instant createdAt) {

    public static DeviceResponse from(Device d) {
        return new DeviceResponse(d.getId(), d.getLabel(), d.getFingerprint(), d.getTrustLevel(),
                d.getLastIp(), d.getLastSeenAt(), d.getCreatedAt());
    }
}
