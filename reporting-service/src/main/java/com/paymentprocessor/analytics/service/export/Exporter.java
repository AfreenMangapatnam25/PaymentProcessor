package com.paymentprocessor.analytics.service.export;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.service.report.ReportData;
import java.io.IOException;
import java.io.OutputStream;

/** Renders {@link ReportData} into a concrete byte format written to {@code out}. */
public interface Exporter {
    ExportFormat format();
    void write(ReportData data, OutputStream out) throws IOException;
}
