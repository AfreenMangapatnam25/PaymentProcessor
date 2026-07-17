package com.paymentprocessor.tokenization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "token")
    private String token;

    @Column(name = "kind")
    private String kind;

    @Column(name = "scope_merchant_id")
    private String scopeMerchantId;

    @Column(name = "fingerprint")
    private byte[] fingerprint;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getScopeMerchantId() { return scopeMerchantId; }
    public void setScopeMerchantId(String scopeMerchantId) { this.scopeMerchantId = scopeMerchantId; }
    public byte[] getFingerprint() { return fingerprint; }
    public void setFingerprint(byte[] fingerprint) { this.fingerprint = fingerprint; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
