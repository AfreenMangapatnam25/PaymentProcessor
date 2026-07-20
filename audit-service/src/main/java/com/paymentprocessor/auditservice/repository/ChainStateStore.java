package com.paymentprocessor.auditservice.repository;

import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.paymentprocessor.auditservice.domain.ChainState;

/**
 * Atomic access to the singleton {@link ChainState} row, backing the hash-chain head.
 * Uses conditional (compare-and-set) updates so concurrent appends — even across
 * multiple service instances — advance the head consistently.
 */
@Repository
public class ChainStateStore {

    private final ChainStateJpaRepository jpa;

    public ChainStateStore(ChainStateJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Returns the current chain head, initialising it to genesis (seq 0) on first use.
     */
    @Transactional
    public ChainState getOrInitialise(String genesisHash) {
        ChainState existing = jpa.findById(ChainState.GLOBAL_ID).orElse(null);
        if (existing != null) {
            return existing;
        }
        ChainState genesis = new ChainState(ChainState.GLOBAL_ID, 0L, genesisHash, Instant.now());
        try {
            jpa.saveAndFlush(genesis);
        } catch (DuplicateKeyException raceLost) {
            // Another instance initialised it first; re-read.
        }
        return jpa.findById(ChainState.GLOBAL_ID).orElseThrow();
    }

    /**
     * Atomically advances the head from {@code expectedSeq}/{@code expectedHead} to
     * {@code newSeq}/{@code newHead}. Returns {@code true} only if this caller won the race.
     */
    @Transactional
    public boolean compareAndAdvance(long expectedSeq, String expectedHead,
                                     long newSeq, String newHead) {
        int updated = jpa.compareAndAdvance(
                ChainState.GLOBAL_ID, expectedSeq, expectedHead, newSeq, newHead, Instant.now());
        return updated == 1;
    }
}
