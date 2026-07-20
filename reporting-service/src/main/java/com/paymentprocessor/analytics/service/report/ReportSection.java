package com.paymentprocessor.analytics.service.report;

import com.paymentprocessor.analytics.service.query.ColumnType;
import java.util.List;

/** One titled table within a report: headers, per-column types, and rows. */
public record ReportSection(
        String title,
        List<String> headers,
        List<ColumnType> types,
        List<List<Object>> rows) {
}
