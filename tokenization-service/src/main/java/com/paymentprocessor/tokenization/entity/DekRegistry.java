package com.paymentprocessor.tokenization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "dek_registry")
public class DekRegistry {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "kek_id")
    private String kekId;

    @Column(name = "wrapped_dek")
    private byte[] wrappedDek;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKekId() { return kekId; }
    public void setKekId(String kekId) { this.kekId = kekId; }
    public byte[] getWrappedDek() { return wrappedDek; }
    public void setWrappedDek(byte[] wrappedDek) { this.wrappedDek = wrappedDek; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getRotatedAt() { return rotatedAt; }
    public void setRotatedAt(Instant rotatedAt) { this.rotatedAt = rotatedAt; }
}
