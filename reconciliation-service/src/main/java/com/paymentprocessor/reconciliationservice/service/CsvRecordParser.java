package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import com.paymentprocessor.reconciliationservice.domain.RecordMatchStatus;
import com.paymentprocessor.reconciliationservice.domain.RecordSource;
import com.paymentprocessor.reconciliationservice.exception.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Header-driven CSV parser that normalizes rows into {@link ReconRecord}s. Column names are matched
 * case-insensitively to record fields; unknown columns are ignored. Required columns:
 * {@code amount}, {@code currency}, {@code transactionDate} (ISO-8601, yyyy-MM-dd).
 *
 * <p>Recognized columns: source, sourceSystem, externalReference (aliases: reference, ref, arn as
 * fallback), arn, internalPaymentId (alias: paymentId), amount, feeAmount, currency, transactionDate
 * (alias: date), valueDate, merchantId, counterparty, cardBin, cardLast4 (alias: last4),
 * transactionStatus (alias: status).
 */
@Component
public class CsvRecordParser {

    public List<ReconRecord> parse(String csv, RecordSource defaultSource, String defaultSourceSystem) {
        if (csv == null || csv.isBlank()) {
            throw new InvalidOperationException("CSV content is empty");
        }
        String[] lines = csv.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        if (lines.length < 2) {
            throw new InvalidOperationException("CSV must contain a header row and at least one data row");
        }

        List<String> headers = splitCsvLine(lines[0]);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(headers.get(i).trim().toLowerCase(), i);
        }

        List<ReconRecord> records = new ArrayList<>();
        for (int row = 1; row < lines.length; row++) {
            String line = lines[row];
            if (line == null || line.isBlank()) {
                continue;
            }
            List<String> cols = splitCsvLine(line);
            records.add(toRecord(cols, index, defaultSource, defaultSourceSystem, row + 1));
        }
        if (records.isEmpty()) {
            throw new InvalidOperationException("CSV contained no data rows");
        }
        return records;
    }

    private ReconRecord toRecord(List<String> cols, Map<String, Integer> index,
                                 RecordSource defaultSource, String defaultSourceSystem, int lineNumber) {
        ReconRecord record = new ReconRecord();

        String sourceValue = get(cols, index, "source");
        record.setSource(sourceValue != null ? parseSource(sourceValue, lineNumber) : defaultSource);
        record.setSourceSystem(firstNonBlank(get(cols, index, "sourcesystem"), defaultSourceSystem));

        record.setExternalReference(firstNonBlank(
                get(cols, index, "externalreference"),
                get(cols, index, "reference"),
                get(cols, index, "ref")));
        record.setArn(get(cols, index, "arn"));
        if (record.getExternalReference() == null) {
            record.setExternalReference(record.getArn());
        }
        record.setInternalPaymentId(firstNonBlank(
                get(cols, index, "internalpaymentid"), get(cols, index, "paymentid")));

        record.setAmount(requireBigDecimal(get(cols, index, "amount"), "amount", lineNumber));
        record.setFeeAmount(parseBigDecimal(get(cols, index, "feeamount")));
        record.setCurrency(require(get(cols, index, "currency"), "currency", lineNumber));
        record.setTransactionDate(requireDate(
                firstNonBlank(get(cols, index, "transactiondate"), get(cols, index, "date")),
                "transactionDate", lineNumber));
        record.setValueDate(parseDate(get(cols, index, "valuedate")));
        record.setMerchantId(get(cols, index, "merchantid"));
        record.setCounterparty(get(cols, index, "counterparty"));
        record.setCardBin(get(cols, index, "cardbin"));
        record.setCardLast4(firstNonBlank(get(cols, index, "cardlast4"), get(cols, index, "last4")));
        record.setTransactionStatus(firstNonBlank(
                get(cols, index, "transactionstatus"), get(cols, index, "status")));
        record.setMatchStatus(RecordMatchStatus.UNMATCHED);
        return record;
    }

    private RecordSource parseSource(String value, int lineNumber) {
        try {
            return RecordSource.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationException(
                    "Invalid source '" + value + "' on line " + lineNumber + " (expected INTERNAL or EXTERNAL)");
        }
    }

    private String get(List<String> cols, Map<String, Integer> index, String name) {
        Integer i = index.get(name);
        if (i == null || i >= cols.size()) {
            return null;
        }
        String value = cols.get(i);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private String require(String value, String field, int lineNumber) {
        if (value == null || value.isBlank()) {
            throw new InvalidOperationException("Missing required column '" + field + "' on line " + lineNumber);
        }
        return value.trim();
    }

    private BigDecimal requireBigDecimal(String value, String field, int lineNumber) {
        BigDecimal parsed = parseBigDecimal(value);
        if (parsed == null) {
            throw new InvalidOperationException(
                    "Missing or invalid numeric column '" + field + "' on line " + lineNumber);
        }
        return parsed;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate requireDate(String value, String field, int lineNumber) {
        LocalDate parsed = parseDate(value);
        if (parsed == null) {
            throw new InvalidOperationException(
                    "Missing or invalid date column '" + field + "' on line " + lineNumber + " (expected yyyy-MM-dd)");
        }
        return parsed;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Minimal RFC-4180-style splitter supporting double-quoted fields and escaped quotes. */
    private List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        result.add(current.toString());
        return result;
    }
}
