package com.paymentprocessor.tokenization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "network_tokens")
public class NetworkToken {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "instrument_id")
    private String instrumentId;

    @Column(name = "network")
    private String network;

    @Column(name = "token_ciphertext")
    private byte[] tokenCiphertext;

    @Column(name = "tar")
    private String tar;

    @Column(name = "exp_month")
    private Short expMonth;

    @Column(name = "exp_year")
    private Short expYear;

    @Column(name = "status")
    private String status;

    @Column(name = "provisioned_at")
    private Instant provisionedAt;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public byte[] getTokenCiphertext() { return tokenCiphertext; }
    public void setTokenCiphertext(byte[] tokenCiphertext) { this.tokenCiphertext = tokenCiphertext; }
    public String getTar() { return tar; }
    public void setTar(String tar) { this.tar = tar; }
    public Short getExpMonth() { return expMonth; }
    public void setExpMonth(Short expMonth) { this.expMonth = expMonth; }
    public Short getExpYear() { return expYear; }
    public void setExpYear(Short expYear) { this.expYear = expYear; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getProvisionedAt() { return provisionedAt; }
    public void setProvisionedAt(Instant provisionedAt) { this.provisionedAt = provisionedAt; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
