package com.paymentprocessor.disputeservice.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Decision recorded by the dispute team when reviewing a piece of evidence.
 */
public class ReviewEvidenceRequest {

    @NotNull
    private Boolean accepted;

    private String reviewer;
    private String notes;

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
