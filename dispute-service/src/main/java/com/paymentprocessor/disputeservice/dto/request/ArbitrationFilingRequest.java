package com.paymentprocessor.disputeservice.dto.request;

/**
 * Command to escalate a dispute to network arbitration after a pre-arbitration
 * rejection.
 */
public class ArbitrationFilingRequest {

    private String narrative;
    private String filedBy;

    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = narrative; }
    public String getFiledBy() { return filedBy; }
    public void setFiledBy(String filedBy) { this.filedBy = filedBy; }
}
