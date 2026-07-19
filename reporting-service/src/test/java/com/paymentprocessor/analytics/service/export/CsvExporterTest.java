package com.paymentprocessor.analytics.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentprocessor.analytics.service.query.ColumnType;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvExporterTest {

    @Test
    void writesHeaderAndEscapedRows() throws Exception {
        ReportSection section = new ReportSection("Results",
                List.of("Merchant", "Amount"),
                List.of(ColumnType.STRING, ColumnType.NUMBER),
                List.of(List.of("mrc,1", 1234.5), List.of("mrc2", 10)));
        ReportData data = new ReportData("t", null, Map.of(), List.of(section));

        var out = new ByteArrayOutputStream();
        new CsvExporter().write(data, out);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertThat(csv).contains("Merchant,Amount");
        assertThat(csv).contains("\"mrc,1\"");   // comma-containing value quoted
        assertThat(csv).contains("1,234.5");     // number formatted with grouping
    }
}
