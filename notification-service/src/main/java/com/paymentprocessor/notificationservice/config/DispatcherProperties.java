package com.paymentprocessor.notificationservice.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the webhook dispatcher, bound from notification.dispatcher.*
 * Defaults match the retry schedule in the design doc: 5s, 30s, 2m, 10m, 1h,
 * 6h, 24h (7 attempts total before an endpoint's delivery is marked dead).
 */
@ConfigurationProperties(prefix = "notification.dispatcher")
public class DispatcherProperties {

    /** How many due deliveries to claim per poll. */
    private int batchSize = 200;

    /** How often the dispatcher polls for due work. */
    private Duration pollInterval = Duration.ofSeconds(2);

    /** How long a claimed row stays invisible to other workers while being delivered. */
    private Duration claimVisibility = Duration.ofMinutes(2);

    /** HTTP timeout for a single delivery attempt. */
    private Duration requestTimeout = Duration.ofSeconds(10);

    /** Backoff delay applied after attempt N fails (1-indexed, matches the design doc). */
    private List<Duration> backoffSchedule = List.of(
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10),
            Duration.ofHours(1),
            Duration.ofHours(6),
            Duration.ofHours(24)
    );

    /** Consecutive endpoint failures before it's auto-disabled. */
    private int autoDisableThreshold = 15;

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public Duration getPollInterval() { return pollInterval; }
    public void setPollInterval(Duration pollInterval) { this.pollInterval = pollInterval; }
    public Duration getClaimVisibility() { return claimVisibility; }
    public void setClaimVisibility(Duration claimVisibility) { this.claimVisibility = claimVisibility; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    public List<Duration> getBackoffSchedule() { return backoffSchedule; }
    public void setBackoffSchedule(List<Duration> backoffSchedule) { this.backoffSchedule = backoffSchedule; }
    public int getAutoDisableThreshold() { return autoDisableThreshold; }
    public void setAutoDisableThreshold(int autoDisableThreshold) { this.autoDisableThreshold = autoDisableThreshold; }

    /**
     * Total attempts allowed before a delivery is marked dead instead of
     * retried: one initial attempt plus one retry per backoff-schedule
     * entry (7 delays -> 8 total attempts).
     */
    public int getMaxAttempts() {
        return backoffSchedule.size() + 1;
    }
}
