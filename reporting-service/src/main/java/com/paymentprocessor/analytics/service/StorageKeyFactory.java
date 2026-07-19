package com.paymentprocessor.analytics.service;

import com.paymentprocessor.analytics.domain.entity.ReportJob;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** Deterministic, collision-free storage keys and download filenames for report files. */
@Component
public class StorageKeyFactory {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC);

    public String keyFor(ReportJob job) {
        String ym = YM.format(job.getCreatedAt());
        return String.format("%s/%s/%s.%s",
                sanitize(job.getMerchantId()), ym, job.getId(), job.getFormat().extension());
    }

    public String filenameFor(ReportJob job) {
        return String.format("%s_%s.%s",
                job.getReportType().name().toLowerCase(), job.getId(), job.getFormat().extension());
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
