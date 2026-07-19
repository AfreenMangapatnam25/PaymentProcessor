package com.paymentprocessor.auditservice.repository;

import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.client.result.UpdateResult;
import com.paymentprocessor.auditservice.domain.ChainState;

/**
 * Atomic access to the singleton {@link ChainState} document, backing the hash-chain
 * head. Uses conditional (compare-and-set) updates so concurrent appends — even across
 * multiple service instances — advance the head consistently.
 */
@Repository
public class ChainStateStore {

    private final MongoTemplate mongo;

    public ChainStateStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Returns the current chain head, initialising it to genesis (seq 0) on first use.
     */
    public ChainState getOrInitialise(String genesisHash) {
        ChainState existing = mongo.findById(ChainState.GLOBAL_ID, ChainState.class);
        if (existing != null) {
            return existing;
        }
        ChainState genesis = new ChainState(ChainState.GLOBAL_ID, 0L, genesisHash, Instant.now());
        try {
            mongo.insert(genesis);
        } catch (DuplicateKeyException raceLost) {
            // Another instance initialised it first; re-read.
        }
        return mongo.findById(ChainState.GLOBAL_ID, ChainState.class);
    }

    /**
     * Atomically advances the head from {@code expectedSeq}/{@code expectedHead} to
     * {@code newSeq}/{@code newHead}. Returns {@code true} only if this caller won the race.
     */
    public boolean compareAndAdvance(long expectedSeq, String expectedHead,
                                     long newSeq, String newHead) {
        Query q = new Query(Criteria.where("_id").is(ChainState.GLOBAL_ID)
                .and("seq").is(expectedSeq)
                .and("headHash").is(expectedHead));
        Update u = new Update()
                .set("seq", newSeq)
                .set("headHash", newHead)
                .set("updatedAt", Instant.now());
        UpdateResult result = mongo.updateFirst(q, u, ChainState.class);
        return result.getModifiedCount() == 1L;
    }
}
