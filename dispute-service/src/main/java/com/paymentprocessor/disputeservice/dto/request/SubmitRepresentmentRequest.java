package com.paymentprocessor.disputeservice.dto.request;

/**
 * Command to assemble and submit a representment package to the network.
 */
public class SubmitRepresentmentRequest {

    private String narrative;
    private String submittedBy;

    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = narrative; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
}
