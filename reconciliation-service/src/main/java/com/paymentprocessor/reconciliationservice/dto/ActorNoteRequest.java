package com.paymentprocessor.reconciliationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Generic actor + note payload (used for defer). */
public record ActorNoteRequest(
        @Size(max = 1024) String note,
        @NotBlank @Size(max = 128) String actor
) {
}
