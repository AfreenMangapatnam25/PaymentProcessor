package com.paymentprocessor.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "channel")
    private String channel;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "recipient_hash")
    private byte[] recipientHash;

    @Column(name = "locale")
    private String locale;

    @Column(name = "status")
    private String status;

    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at")
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public byte[] getRecipientHash() { return recipientHash; }
    public void setRecipientHash(byte[] recipientHash) { this.recipientHash = recipientHash; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderRef() { return providerRef; }
    public void setProviderRef(String providerRef) { this.providerRef = providerRef; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
