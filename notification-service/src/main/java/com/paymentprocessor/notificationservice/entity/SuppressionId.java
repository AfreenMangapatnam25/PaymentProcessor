package com.paymentprocessor.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

@Embeddable
public class SuppressionId implements Serializable {

    @Column(name = "channel")
    private String channel;

    @Column(name = "recipient_hash")
    private byte[] recipientHash;

    public SuppressionId() {
    }

    public SuppressionId(String channel, byte[] recipientHash) {
        this.channel = channel;
        this.recipientHash = recipientHash;
    }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public byte[] getRecipientHash() { return recipientHash; }
    public void setRecipientHash(byte[] recipientHash) { this.recipientHash = recipientHash; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SuppressionId)) return false;
        SuppressionId that = (SuppressionId) o;
        return Objects.equals(channel, that.channel) && Arrays.equals(recipientHash, that.recipientHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, Arrays.hashCode(recipientHash));
    }
}
