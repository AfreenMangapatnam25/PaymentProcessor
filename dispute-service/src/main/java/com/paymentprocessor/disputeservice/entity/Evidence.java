package com.paymentprocessor.disputeservice.entity;

import com.paymentprocessor.disputeservice.domain.enums.EvidenceCategory;
import com.paymentprocessor.disputeservice.domain.enums.EvidenceStatus;
import com.paymentprocessor.disputeservice.domain.enums.EvidenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A single piece of merchant-supplied evidence attached to a dispute. Documents
 * are stored externally; this record holds the metadata, categorisation and
 * review state used when assembling a representment package.
 */
@Entity
@Table(name = "evidence", indexes = {
        @Index(name = "idx_evidence_dispute", columnList = "dispute_id")
})
public class Evidence {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "dispute_id", nullable = false)
    private String disputeId;

    @Column(name = "file_name")
    private String fileName;

    /** Key of the encrypted object in the document store. */
    @Column(name = "storage_key")
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private EvidenceType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private EvidenceCategory category;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Hex-encoded SHA-256 content digest for integrity verification. */
    @Column(name = "sha256", length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private EvidenceStatus status = EvidenceStatus.UPLOADED;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "malware_scanned")
    private boolean malwareScanned;

    @Lob
    @Column(name = "ocr_text")
    private String ocrText;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = "evd_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public EvidenceType getType() { return type; }
    public void setType(EvidenceType type) { this.type = type; }
    public EvidenceCategory getCategory() { return category; }
    public void setCategory(EvidenceCategory category) { this.category = category; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public EvidenceStatus getStatus() { return status; }
    public void setStatus(EvidenceStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isMalwareScanned() { return malwareScanned; }
    public void setMalwareScanned(boolean malwareScanned) { this.malwareScanned = malwareScanned; }
    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
