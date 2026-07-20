package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.KybDocumentType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Registers metadata for a document already uploaded to object storage. */
public record KybDocumentRequest(
        @NotNull KybDocumentType documentType,
        @Size(max = 255) String fileName,
        @Size(max = 120) String contentType,
        @NotBlank @Size(max = 512) String storageReference,
        LocalDate expiresOn
) {}
