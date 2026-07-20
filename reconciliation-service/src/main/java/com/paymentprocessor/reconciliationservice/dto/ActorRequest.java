package com.paymentprocessor.reconciliationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Generic actor payload (used for approve / reject / post). */
public record ActorRequest(
        @NotBlank @Size(max = 128) String actor
) {
}
