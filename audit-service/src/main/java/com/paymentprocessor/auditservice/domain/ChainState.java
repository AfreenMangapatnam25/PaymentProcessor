package com.paymentprocessor.auditservice.domain;

import java.time.Instant;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Singleton row (id = {@link #GLOBAL_ID}) tracking the head of the global hash
 * chain: the most recent sequence number and its hash. Advanced with a compare-and-set
 * update so concurrent appends stay consistent.
 */
@Entity
@Table(name = "audit_chain_state")
public class ChainState implements Persistable<String> {

    public static final String GLOBAL_ID = "GLOBAL";

    @Id
    private String id;

    /** Sequence number of the head record (0 = genesis, no records yet). */
    private long seq;

    /** Hash of the head record, or the genesis hash when seq == 0. */
    @Column(name = "head_hash")
    private String headHash;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Transient
    private transient boolean isNew = true;

    public ChainState() {
    }

    public ChainState(String id, long seq, String headHash, Instant updatedAt) {
        this.id = id;
        this.seq = seq;
        this.headHash = headHash;
        this.updatedAt = updatedAt;
    }

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
    public String getHeadHash() { return headHash; }
    public void setHeadHash(String headHash) { this.headHash = headHash; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean isNew() { return isNew; }

    @PostPersist
    @PostLoad
    void markNotNew() { this.isNew = false; }
}
