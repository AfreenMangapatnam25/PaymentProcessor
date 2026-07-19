package com.paymentprocessor.analytics.service.report;

import java.util.List;
import java.util.Map;

/**
 * The rendered, format-agnostic content of a report. Exporters turn this into
 * CSV/PDF/Excel. A report may contain several sections (e.g. a summary block plus a
 * detail table).
 */
public record ReportData(
        String title,
        String subtitle,
        Map<String, String> metadata,
        List<ReportSection> sections) {

    public long totalRows() {
        return sections.stream().mapToLong(s -> s.rows().size()).sum();
    }
}
