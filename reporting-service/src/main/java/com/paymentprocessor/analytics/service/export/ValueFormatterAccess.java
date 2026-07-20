package com.paymentprocessor.analytics.service.export;

import com.paymentprocessor.analytics.service.query.ColumnType;

final class ValueFormatterAccess {
    private ValueFormatterAccess() { }
    static String format(Object v, ColumnType t) { return ValueFormatter.asString(v, t); }
    static Double number(Object v) { return ValueFormatter.asNumber(v); }
}
