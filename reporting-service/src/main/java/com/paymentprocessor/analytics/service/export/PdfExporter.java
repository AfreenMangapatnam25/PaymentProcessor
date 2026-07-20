package com.paymentprocessor.analytics.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.service.query.ColumnType;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Landscape A4 PDF via OpenPDF. Intended for summary-style templates rather than huge
 * detail exports; detail rows are capped to keep the document reasonable.
 */
@Component
public class PdfExporter implements Exporter {

    private static final int MAX_ROWS_PER_SECTION = 5_000;
    private static final Color HEADER_BG = new Color(0x1F, 0x3A, 0x5F);
    private static final Color ZEBRA = new Color(0xF2, 0xF5, 0xF9);

    @Override
    public ExportFormat format() { return ExportFormat.PDF; }

    @Override
    public void write(ReportData data, OutputStream out) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 32, 32);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(0x1F, 0x3A, 0x5F));
            doc.add(new Paragraph(data.title() != null ? data.title() : "Report", titleFont));
            if (data.subtitle() != null) {
                doc.add(new Paragraph(data.subtitle(),
                        FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY)));
            }
            if (data.metadata() != null && !data.metadata().isEmpty()) {
                Font meta = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> e : data.metadata().entrySet()) {
                    if (sb.length() > 0) sb.append("   ");
                    sb.append(e.getKey()).append(": ").append(e.getValue());
                }
                doc.add(new Paragraph(sb.toString(), meta));
            }
            doc.add(new Paragraph(" "));

            for (ReportSection section : data.sections()) {
                if (section.title() != null) {
                    doc.add(new Paragraph(section.title(),
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                    doc.add(new Paragraph(" "));
                }
                doc.add(buildTable(section));
                doc.add(new Paragraph(" "));
            }

            if (data.sections().stream().anyMatch(s -> s.rows().size() > MAX_ROWS_PER_SECTION)) {
                doc.add(new Paragraph("Note: table truncated for PDF; use CSV or Excel for the full dataset.",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
            }
            doc.close();
        } catch (RuntimeException ex) {
            throw new IOException("PDF generation failed", ex);
        }
    }

    private PdfPTable buildTable(ReportSection section) {
        int cols = section.headers().size();
        PdfPTable table = new PdfPTable(Math.max(cols, 1));
        table.setWidthPercentage(100);

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : section.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(4);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
        table.setHeaderRows(1);

        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        List<ColumnType> types = section.types();
        int rowIdx = 0;
        for (List<Object> row : section.rows()) {
            if (rowIdx >= MAX_ROWS_PER_SECTION) break;
            boolean zebra = (rowIdx++ % 2) == 1;
            for (int c = 0; c < cols; c++) {
                ColumnType t = types != null && c < types.size() ? types.get(c) : ColumnType.STRING;
                Object v = c < row.size() ? row.get(c) : null;
                PdfPCell cell = new PdfPCell(new Phrase(ValueFormatterAccess.format(v, t), bodyFont));
                cell.setPadding(3);
                if (zebra) cell.setBackgroundColor(ZEBRA);
                if (t == ColumnType.NUMBER) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);
            }
        }
        return table;
    }
}
