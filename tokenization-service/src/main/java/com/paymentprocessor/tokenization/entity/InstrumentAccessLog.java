package com.paymentprocessor.tokenization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "instrument_access_log")
public class InstrumentAccessLog {

    @Id
    private Long id;

    @Column(name = "instrument_id")
    private String instrumentId;

    @Column(name = "actor")
    private String actor;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
