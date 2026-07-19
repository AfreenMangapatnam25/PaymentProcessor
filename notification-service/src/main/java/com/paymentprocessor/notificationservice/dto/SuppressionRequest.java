package com.paymentprocessor.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public class SuppressionRequest {

    @NotBlank
    private String channel;

    @NotBlank
    private String recipient;

    @NotBlank
    private String reason;   // bounce|complaint|unsubscribe

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
