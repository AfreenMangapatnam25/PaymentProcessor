package com.paymentprocessor.disputeservice.dto.request;

import com.paymentprocessor.disputeservice.domain.enums.EvidenceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Metadata for an evidence document upload. The binary content is assumed to be
 * streamed to the document store separately; this request registers it.
 */
public class UploadEvidenceRequest {

    @NotBlank
    private String fileName;

    private String storageKey;

    @NotNull
    private EvidenceCategory category;

    @NotNull
    @Positive
    private Long sizeBytes;

    private String sha256;
    private String description;
    private String uploadedBy;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public EvidenceCategory getCategory() { return category; }
    public void setCategory(EvidenceCategory category) { this.category = category; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
