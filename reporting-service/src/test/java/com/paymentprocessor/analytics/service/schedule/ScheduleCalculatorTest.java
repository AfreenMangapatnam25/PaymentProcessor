package com.paymentprocessor.analytics.service.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentprocessor.analytics.domain.enums.ScheduleCadence;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ScheduleCalculatorTest {

    private final ScheduleCalculator calc = new ScheduleCalculator();

    @Test
    void dailyWindowIsYesterday() {
        Instant runAt = ZonedDateTime.of(2026, 7, 17, 2, 0, 0, 0, ZoneOffset.UTC).toInstant();
        var w = calc.windowFor(ScheduleCadence.DAILY, "UTC", runAt);
        assertThat(w.from()).isEqualTo(LocalDate.of(2026, 7, 16));
        assertThat(w.to()).isEqualTo(LocalDate.of(2026, 7, 16));
    }

    @Test
    void monthlyWindowIsPreviousCalendarMonth() {
        Instant runAt = ZonedDateTime.of(2026, 7, 1, 2, 0, 0, 0, ZoneOffset.UTC).toInstant();
        var w = calc.windowFor(ScheduleCadence.MONTHLY, "UTC", runAt);
        assertThat(w.from()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(w.to()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void nextRunIsInTheFuture() {
        Instant now = Instant.parse("2026-07-17T10:00:00Z");
        Instant next = calc.nextRun(ScheduleCadence.DAILY, null, "UTC", now);
        assertThat(next).isAfter(now);
    }

    @Test
    void cronOverrideIsHonoured() {
        Instant now = Instant.parse("2026-07-17T10:00:00Z");
        Instant next = calc.nextRun(ScheduleCadence.DAILY, "0 0 6 * * *", "UTC", now);
        assertThat(next).isEqualTo(Instant.parse("2026-07-18T06:00:00Z"));
    }
}
