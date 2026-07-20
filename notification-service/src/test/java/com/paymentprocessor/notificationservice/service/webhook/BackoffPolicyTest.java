package com.paymentprocessor.notificationservice.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentprocessor.notificationservice.config.DispatcherProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class BackoffPolicyTest {

    @Test
    void followsTheConfiguredScheduleInOrder() {
        DispatcherProperties props = new DispatcherProperties();
        props.setBackoffSchedule(List.of(
                Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofMinutes(2)));
        BackoffPolicy policy = new BackoffPolicy(props);

        assertThat(policy.delayFor(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(policy.delayFor(2)).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.delayFor(3)).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void isExhaustedOnceScheduleIsConsumed() {
        DispatcherProperties props = new DispatcherProperties();
        props.setBackoffSchedule(List.of(Duration.ofSeconds(5), Duration.ofSeconds(30)));
        BackoffPolicy policy = new BackoffPolicy(props);

        assertThat(policy.isExhausted(1)).isFalse();
        assertThat(policy.isExhausted(2)).isFalse();
        assertThat(policy.isExhausted(3)).isTrue();
        assertThat(policy.delayFor(3)).isNull();
    }

    @Test
    void defaultScheduleMatchesDesignDoc() {
        DispatcherProperties props = new DispatcherProperties();
        List<Duration> schedule = props.getBackoffSchedule();

        assertThat(schedule).containsExactly(
                Duration.ofSeconds(5),
                Duration.ofSeconds(30),
                Duration.ofMinutes(2),
                Duration.ofMinutes(10),
                Duration.ofHours(1),
                Duration.ofHours(6),
                Duration.ofHours(24)
        );
        assertThat(props.getMaxAttempts()).isEqualTo(8); // 1 initial + 7 retries
    }
}
