package com.paymentprocessor.userservice.common.util;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final ZoneId UTC = ZoneId.of("UTC");

    public static Instant nowUtc() {
        return Instant.now();
    }

    public static Instant toUtc(Instant instant) {
        return instant != null ? instant.atZone(UTC).toInstant() : null;
    }

    public static String formatIso(Instant instant) {
        return instant != null ? ISO_FORMATTER.format(instant) : null;
    }

    public static Instant parseIso(String isoString) {
        return isoString != null ? Instant.parse(isoString) : null;
    }

    public static ZonedDateTime toZonedDateTime(Instant instant) {
        return instant != null ? instant.atZone(UTC) : null;
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, UTC) : null;
    }

    public static Instant fromLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(UTC).toInstant() : null;
    }
}