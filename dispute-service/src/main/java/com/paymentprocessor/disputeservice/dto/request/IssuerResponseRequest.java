package com.paymentprocessor.disputeservice.dto.request;

import com.paymentprocessor.disputeservice.domain.enums.IssuerResponse;
import jakarta.validation.constraints.NotNull;

/**
 * Records the issuer's decision on a submitted representment.
 */
public class IssuerResponseRequest {

    @NotNull
    private IssuerResponse response;

    private String actor;
    private String notes;

    public IssuerResponse getResponse() { return response; }
    public void setResponse(IssuerResponse response) { this.response = response; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
