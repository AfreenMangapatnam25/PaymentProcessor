package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.KybDocumentStatus;
import com.paymentprocessor.merchantservice.common.enums.KybDocumentType;
import java.time.LocalDate;
import java.util.UUID;

public record KybDocumentResponse(
        UUID id, UUID merchantId, UUID kybCaseId, KybDocumentType documentType,
        String fileName, String contentType, String storageReference,
        KybDocumentStatus status, LocalDate expiresOn
) {}
