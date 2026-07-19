package com.paymentprocessor.analytics.service.export;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.service.query.ColumnType;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

/** Streaming XLSX via POI SXSSF (bounded memory). One sheet per report section. */
@Component
public class ExcelExporter implements Exporter {

    @Override
    public ExportFormat format() { return ExportFormat.EXCEL; }

    @Override
    public void write(ReportData data, OutputStream out) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(500)) {
            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            int sheetIdx = 0;
            for (ReportSection section : data.sections()) {
                String baseName = section.title() != null ? section.title() : "Sheet" + (sheetIdx + 1);
                String safeName = WorkbookUtil.createSafeSheetName(baseName + " " + (++sheetIdx));
                SXSSFSheet sheet = wb.createSheet(safeName);
                sheet.trackAllColumnsForAutoSizing();

                int r = 0;
                Row header = sheet.createRow(r++);
                for (int c = 0; c < section.headers().size(); c++) {
                    Cell cell = header.createCell(c);
                    cell.setCellValue(section.headers().get(c));
                    cell.setCellStyle(headerStyle);
                }

                List<ColumnType> types = section.types();
                for (List<Object> dataRow : section.rows()) {
                    Row row = sheet.createRow(r++);
                    for (int c = 0; c < dataRow.size(); c++) {
                        Cell cell = row.createCell(c);
                        ColumnType t = types != null && c < types.size() ? types.get(c) : ColumnType.STRING;
                        Object v = dataRow.get(c);
                        Double num = t == ColumnType.NUMBER ? ValueFormatterAccess.number(v) : null;
                        if (num != null) {
                            cell.setCellValue(num);
                        } else {
                            cell.setCellValue(ValueFormatterAccess.format(v, t));
                        }
                    }
                }
                if (r > 1) sheet.createFreezePane(0, 1);
            }
            wb.write(out);
            wb.dispose();
        }
    }
}
