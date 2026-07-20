package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.KybDocumentStatus;
import com.paymentprocessor.merchantservice.common.enums.KybDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Metadata for a document uploaded during onboarding/verification. The binary itself lives in
 * object storage; only a storage reference is retained here.
 */
@Entity
@Table(name = "kyb_document", indexes = {
        @Index(name = "ix_kybdoc_merchant", columnList = "merchant_id"),
        @Index(name = "ix_kybdoc_case", columnList = "kyb_case_id")
})
@Getter
@Setter
public class KybDocument extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "kyb_case_id")
    private UUID kybCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private KybDocumentType documentType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    /** Opaque reference into object storage (e.g. S3 key). */
    @Column(name = "storage_reference", nullable = false, length = 512)
    private String storageReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KybDocumentStatus status = KybDocumentStatus.UPLOADED;

    @Column(name = "expires_on")
    private LocalDate expiresOn;
}
