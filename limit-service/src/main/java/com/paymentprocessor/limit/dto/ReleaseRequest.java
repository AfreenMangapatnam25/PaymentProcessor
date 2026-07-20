package com.paymentprocessor.limit.dto;

import jakarta.validation.constraints.Size;

/**
 * Request to release a reservation (transaction failed, cancelled, expired, or a
 * manual override). The reason is recorded on the emitted event and audit trail.
 */
public record ReleaseRequest(
        @Size(max = 200)
        String reason
) {}
