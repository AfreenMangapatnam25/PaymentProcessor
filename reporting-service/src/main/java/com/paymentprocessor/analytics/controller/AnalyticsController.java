package com.paymentprocessor.analytics.controller;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.service.template.TemplateRegistry;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Service", description = "Service metadata and capabilities")
public class AnalyticsController {

    private final TemplateRegistry templates;

    public AnalyticsController(TemplateRegistry templates) {
        this.templates = templates;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "service", "analytics",
                "store", "clickhouse",
                "role", "read-only CDC replica",
                "reportTypes", templates.supportedTypes().stream().map(Enum::name).sorted().toList(),
                "formats", Arrays.stream(ExportFormat.values()).map(Enum::name).toList());
    }
}
