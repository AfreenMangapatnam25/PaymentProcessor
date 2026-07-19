package com.paymentprocessor.auditservice.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Singleton document (_id = {@link #GLOBAL_ID}) tracking the head of the global hash
 * chain: the most recent sequence number and its hash. Advanced with a compare-and-set
 * update so concurrent appends stay consistent.
 */
@Document(collection = "audit_chain_state")
public class ChainState {

    public static final String GLOBAL_ID = "GLOBAL";

    @Id
    private String id;

    /** Sequence number of the head record (0 = genesis, no records yet). */
    private long seq;

    /** Hash of the head record, or the genesis hash when seq == 0. */
    private String headHash;

    private Instant updatedAt;

    public ChainState() {
    }

    public ChainState(String id, long seq, String headHash, Instant updatedAt) {
        this.id = id;
        this.seq = seq;
        this.headHash = headHash;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
    public String getHeadHash() { return headHash; }
    public void setHeadHash(String headHash) { this.headHash = headHash; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
