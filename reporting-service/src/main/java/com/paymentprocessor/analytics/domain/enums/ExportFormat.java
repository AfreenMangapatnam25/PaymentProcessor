package com.paymentprocessor.analytics.domain.enums;

/** Supported output encodings. */
public enum ExportFormat {
    CSV("text/csv", "csv"),
    PDF("application/pdf", "pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String contentType;
    private final String extension;

    ExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() { return contentType; }
    public String extension() { return extension; }
}
