package com.paymentprocessor.disputeservice.dto.request;

import com.paymentprocessor.disputeservice.domain.enums.ArbitrationOutcome;
import jakarta.validation.constraints.NotNull;

/**
 * Records the network's binding arbitration decision.
 */
public class ArbitrationDecisionRequest {

    @NotNull
    private ArbitrationOutcome outcome;

    private String actor;
    private String notes;

    public ArbitrationOutcome getOutcome() { return outcome; }
    public void setOutcome(ArbitrationOutcome outcome) { this.outcome = outcome; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
