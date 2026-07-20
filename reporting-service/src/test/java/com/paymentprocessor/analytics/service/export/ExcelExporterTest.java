package com.paymentprocessor.analytics.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentprocessor.analytics.service.query.ColumnType;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelExporterTest {

    @Test
    void producesReadableWorkbookWithNumericCells() throws Exception {
        ReportSection section = new ReportSection("Fees",
                List.of("Connector", "Fee"),
                List.of(ColumnType.STRING, ColumnType.NUMBER),
                List.of(List.of("stripe", 999L)));
        ReportData data = new ReportData("Fee Breakdown", null, Map.of(), List.of(section));

        var out = new ByteArrayOutputStream();
        new ExcelExporter().write(data, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Connector");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(999.0);
        }
    }
}
