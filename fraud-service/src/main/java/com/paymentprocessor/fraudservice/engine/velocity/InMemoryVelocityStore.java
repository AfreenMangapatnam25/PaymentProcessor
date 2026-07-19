package com.paymentprocessor.fraudservice.engine.velocity;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory {@link VelocityStore}. Keeps a bounded deque of event timestamps per
 * key and prunes entries outside the query window on each access.
 *
 * <p>Not distributed and not persistent — intended as a drop-in default that can
 * be swapped for a Redis-backed implementation without touching the evaluators.
 */
@Component
public class InMemoryVelocityStore implements VelocityStore {

    /** Hard cap per key to bound memory even under abusive traffic. */
    private static final int MAX_ENTRIES_PER_KEY = 2048;

    private final Map<String, Deque<Long>> events = new ConcurrentHashMap<>();

    @Override
    public int hit(String key, Duration window) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(now);
            if (deque.size() > MAX_ENTRIES_PER_KEY) {
                deque.pollFirst();
            }
            prune(deque, now - window.toMillis());
            return deque.size();
        }
    }

    @Override
    public int count(String key, Duration window) {
        Deque<Long> deque = events.get(key);
        if (deque == null) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - window.toMillis();
        synchronized (deque) {
            prune(deque, cutoff);
            return deque.size();
        }
    }

    private void prune(Deque<Long> deque, long cutoff) {
        Long head;
        while ((head = deque.peekFirst()) != null && head < cutoff) {
            deque.pollFirst();
        }
    }
}
