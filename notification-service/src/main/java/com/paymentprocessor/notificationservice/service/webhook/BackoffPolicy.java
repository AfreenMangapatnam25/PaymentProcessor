package com.paymentprocessor.notificationservice.service.webhook;

import com.paymentprocessor.notificationservice.config.DispatcherProperties;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Translates a (1-indexed) attempt number into "how long until the next
 * retry", per notification.dispatcher.backoff-schedule (default: 5s, 30s,
 * 2m, 10m, 1h, 6h, 24h). Returns empty once attempts are exhausted, meaning
 * the delivery should be marked dead rather than retried again.
 */
@Component
public class BackoffPolicy {

    private final DispatcherProperties properties;

    public BackoffPolicy(DispatcherProperties properties) {
        this.properties = properties;
    }

    public Duration delayFor(int attempt) {
        var schedule = properties.getBackoffSchedule();
        if (attempt < 1 || attempt > schedule.size()) {
            return null;
        }
        return schedule.get(attempt - 1);
    }

    public boolean isExhausted(int attempt) {
        return delayFor(attempt) == null;
    }
}
