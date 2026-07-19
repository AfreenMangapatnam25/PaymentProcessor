package com.paymentprocessor.analytics.service.export;

import com.paymentprocessor.analytics.service.query.ColumnType;
import java.text.NumberFormat;
import java.util.Locale;

/** Shared cell formatting so every export renders values consistently. */
final class ValueFormatter {

    private ValueFormatter() { }

    static String asString(Object value, ColumnType type) {
        if (value == null) return "";
        if (type == ColumnType.NUMBER && value instanceof Number n) {
            NumberFormat f = NumberFormat.getNumberInstance(Locale.US);
            f.setGroupingUsed(true);
            f.setMaximumFractionDigits(4);
            return f.format(n);
        }
        return String.valueOf(value);
    }

    static Double asNumber(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return null;
    }
}
