package com.paymentprocessor.fraudservice.engine.velocity;

import java.time.Duration;

/**
 * Sliding-window counter used for velocity checks. The reference implementation
 * is in-memory; in production this would be backed by Redis with TTLs so the
 * counters are shared across service instances.
 */
public interface VelocityStore {

    /**
     * Record an occurrence for {@code key} at "now" and return the number of
     * occurrences within the given window (including the one just recorded).
     */
    int hit(String key, Duration window);

    /** Count occurrences within the window without recording a new one. */
    int count(String key, Duration window);
}
