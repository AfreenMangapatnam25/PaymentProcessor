package com.paymentprocessor.userservice.common.util;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class ClockProvider {

    private final Clock clock;

    public ClockProvider() {
        this.clock = Clock.systemUTC();
    }

    public ClockProvider(Clock clock) {
        this.clock = clock;
    }

    public Clock clock() {
        return clock;
    }

    public Instant now() {
        return clock.instant();
    }

    public ZonedDateTime nowZoned() {
        return ZonedDateTime.now(clock);
    }

    public long nowMillis() {
        return clock.millis();
    }

    public Clock withZone(ZoneId zone) {
        return clock.withZone(zone);
    }
}