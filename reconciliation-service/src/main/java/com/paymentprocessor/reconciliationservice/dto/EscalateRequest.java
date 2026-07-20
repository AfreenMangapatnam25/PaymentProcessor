package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.ReviewQueue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Escalate an exception, optionally re-routing it to another review queue. */
public record EscalateRequest(
        ReviewQueue queue,
        @Size(max = 1024) String note,
        @NotBlank @Size(max = 128) String actor
) {
}
