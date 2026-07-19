package com.paymentprocessor.analytics.service.schedule;

import com.paymentprocessor.analytics.domain.enums.ScheduleCadence;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Computes the next fire time for a schedule and the reporting window each run should
 * cover (always the previous complete period, so data is settled).
 */
@Component
public class ScheduleCalculator {

    /** Default fire time: 02:00 local. */
    private static final int RUN_HOUR = 2;

    public Instant nextRun(ScheduleCadence cadence, String cron, String timezone, Instant after) {
        ZoneId zone = resolveZone(timezone);
        ZonedDateTime from = ZonedDateTime.ofInstant(after, zone);
        if (StringUtils.hasText(cron)) {
            ZonedDateTime next = CronExpression.parse(cron).next(from);
            if (next == null) {
                throw new IllegalArgumentException("Cron expression never fires: " + cron);
            }
            return next.toInstant();
        }
        ZonedDateTime base = from.withHour(RUN_HOUR).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime next = switch (cadence) {
            case DAILY -> base.isAfter(from) ? base : base.plusDays(1);
            case WEEKLY -> {
                ZonedDateTime monday = base.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
                yield monday.isAfter(from) ? monday : monday.plusWeeks(1);
            }
            case MONTHLY -> {
                ZonedDateTime first = base.with(TemporalAdjusters.firstDayOfMonth());
                yield first.isAfter(from) ? first : first.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            }
        };
        return next.toInstant();
    }

    /** The [from, to] date window a run at {@code runAt} should report on. */
    public DateWindow windowFor(ScheduleCadence cadence, String timezone, Instant runAt) {
        ZoneId zone = resolveZone(timezone);
        LocalDate today = ZonedDateTime.ofInstant(runAt, zone).toLocalDate();
        return switch (cadence) {
            case DAILY -> {
                LocalDate yesterday = today.minusDays(1);
                yield new DateWindow(yesterday, yesterday);
            }
            case WEEKLY -> {
                LocalDate lastMonday = today.with(TemporalAdjusters.previous(java.time.DayOfWeek.MONDAY));
                yield new DateWindow(lastMonday, lastMonday.plusDays(6));
            }
            case MONTHLY -> {
                LocalDate firstOfThis = today.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate firstOfPrev = firstOfThis.minusMonths(1);
                yield new DateWindow(firstOfPrev, firstOfThis.minusDays(1));
            }
        };
    }

    private ZoneId resolveZone(String tz) {
        return StringUtils.hasText(tz) ? ZoneId.of(tz) : ZoneId.of("UTC");
    }

    public record DateWindow(LocalDate from, LocalDate to) { }
}
