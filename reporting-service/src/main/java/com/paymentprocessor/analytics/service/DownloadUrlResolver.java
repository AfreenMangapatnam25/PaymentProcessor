package com.paymentprocessor.analytics.service;

import com.paymentprocessor.analytics.domain.entity.ReportJob;
import com.paymentprocessor.analytics.service.storage.StorageService;
import org.springframework.stereotype.Component;

/** Produces the best available download URL: S3 presigned when possible, else the API path. */
@Component
public class DownloadUrlResolver {

    private final StorageService storage;
    private final StorageKeyFactory keys;

    public DownloadUrlResolver(StorageService storage, StorageKeyFactory keys) {
        this.storage = storage;
        this.keys = keys;
    }

    public String resolve(ReportJob job) {
        if (job.getStorageKey() == null) {
            return null;
        }
        return storage.presignedUrl(job.getStorageKey(), keys.filenameFor(job), job.getFormat().contentType())
                .orElse("/api/v1/reports/" + job.getId() + "/download");
    }
}
