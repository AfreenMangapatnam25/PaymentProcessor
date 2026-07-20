package com.paymentprocessor.analytics.service.export;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.service.query.ColumnType;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/** RFC-4180 CSV. Multi-section reports are emitted as blank-line-separated blocks. */
@Component
public class CsvExporter implements Exporter {

    @Override
    public ExportFormat format() { return ExportFormat.CSV; }

    @Override
    public void write(ReportData data, OutputStream out) throws IOException {
        Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        // Excel-friendly UTF-8 BOM
        w.write('\uFEFF');
        boolean first = true;
        for (ReportSection section : data.sections()) {
            if (!first) w.write("\r\n");
            first = false;
            if (data.sections().size() > 1 && section.title() != null) {
                w.write(escape("# " + section.title()));
                w.write("\r\n");
            }
            writeRow(w, section.headers());
            List<ColumnType> types = section.types();
            for (List<Object> row : section.rows()) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) line.append(',');
                    ColumnType t = types != null && i < types.size() ? types.get(i) : ColumnType.STRING;
                    line.append(escape(ValueFormatterAccess.format(row.get(i), t)));
                }
                w.write(line.toString());
                w.write("\r\n");
            }
        }
        w.flush();
    }

    private void writeRow(Writer w, List<String> cells) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) line.append(',');
            line.append(escape(cells.get(i)));
        }
        w.write(line.toString());
        w.write("\r\n");
    }

    private String escape(String v) {
        if (v == null) return "";
        boolean needsQuote = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String s = v.replace("\"", "\"\"");
        return needsQuote ? "\"" + s + "\"" : s;
    }
}
