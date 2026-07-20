package com.paymentprocessor.analytics.controller;

import com.paymentprocessor.analytics.domain.entity.ReportJob;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import com.paymentprocessor.analytics.dto.CreateReportRequest;
import com.paymentprocessor.analytics.dto.ReportJobResponse;
import com.paymentprocessor.analytics.exception.ReportNotReadyException;
import com.paymentprocessor.analytics.service.ReportService;
import com.paymentprocessor.analytics.service.StorageKeyFactory;
import com.paymentprocessor.analytics.service.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Async report generation and download")
public class ReportController {

    private final ReportService reports;
    private final StorageService storage;
    private final StorageKeyFactory keys;

    public ReportController(ReportService reports, StorageService storage, StorageKeyFactory keys) {
        this.reports = reports;
        this.storage = storage;
        this.keys = keys;
    }

    @PostMapping
    @Operation(summary = "Submit an async report; returns 202 with the QUEUED job")
    public ResponseEntity<ReportJobResponse> submit(@Valid @RequestBody CreateReportRequest req,
                                                    UriComponentsBuilder uri) {
        ReportJob job = reports.submit(req);
        URI location = uri.path("/api/v1/reports/{id}").buildAndExpand(job.getId()).toUri();
        return ResponseEntity.accepted().location(location).body(ReportJobResponse.from(job));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report job status/metadata")
    public ReportJobResponse get(@PathVariable("id") java.util.UUID id) {
        ReportJob job = reports.get(id);
        return ReportJobResponse.from(job, reports.downloadUrl(job));
    }

    @GetMapping
    @Operation(summary = "List a merchant's report jobs (paged)")
    public Page<ReportJobResponse> list(@RequestParam("merchantId") String merchantId,
                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "20") int size) {
        return reports.listForMerchant(merchantId, PageRequest.of(page, Math.min(size, 100)))
                .map(j -> ReportJobResponse.from(j, reports.downloadUrl(j)));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download the generated file (redirects to presigned URL when on S3)")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable("id") java.util.UUID id) {
        ReportJob job = reports.get(id);
        if (job.getStatus() != ReportStatus.COMPLETED || job.getStorageKey() == null) {
            throw new ReportNotReadyException("Report " + id + " is " + job.getStatus() + ", not available");
        }
        String filename = keys.filenameFor(job);
        String contentType = job.getFormat().contentType();

        Optional<String> presigned = storage.presignedUrl(job.getStorageKey(), filename, contentType);
        if (presigned.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(presigned.get())).build();
        }

        StreamingResponseBody body = out -> {
            try (InputStream in = storage.open(job.getStorageKey())) {
                in.transferTo(out);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
