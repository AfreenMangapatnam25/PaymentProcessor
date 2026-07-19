package com.paymentprocessor.limit.service;

import com.paymentprocessor.limit.domain.enums.TimeWindow;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * Computes the window instance ({@code windowKey}) and its start instant for a
 * limit's {@link TimeWindow}, honouring the entity's configured time zone. The
 * window key is the natural unique identifier of a counter bucket, e.g.
 * "2026-07-18" for a daily limit or "2026-W29" for a weekly one.
 */
@Component
public class WindowResolver {

    public record Window(String key, Instant start) {}

    public Window resolve(TimeWindow window, String timeZone, Instant at) {
        ZoneId zone = ZoneId.of(timeZone == null ? "UTC" : timeZone);
        ZonedDateTime now = at.atZone(zone);

        return switch (window) {
            case DAILY -> {
                ZonedDateTime start = now.toLocalDate().atStartOfDay(zone);
                yield new Window(now.toLocalDate().toString(), start.toInstant());
            }
            case WEEKLY -> {
                LocalDate monday = now.toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                int week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int weekYear = now.get(IsoFields.WEEK_BASED_YEAR);
                String key = "%d-W%02d".formatted(weekYear, week);
                yield new Window(key, monday.atStartOfDay(zone).toInstant());
            }
            case MONTHLY -> {
                ZonedDateTime start = now.toLocalDate()
                        .withDayOfMonth(1).atStartOfDay(zone);
                String key = "%d-%02d".formatted(now.getYear(), now.getMonthValue());
                yield new Window(key, start.toInstant());
            }
            case LIFETIME -> new Window("ALL", Instant.EPOCH);
            case PER_TRANSACTION ->
                // Stateless — evaluated per request, never accumulated.
                    new Window("PER_TXN", at);
        };
    }
}
