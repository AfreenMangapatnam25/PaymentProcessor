package com.paymentprocessor.analytics.service.export;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExporterFactory {

    private final Map<ExportFormat, Exporter> exporters = new EnumMap<>(ExportFormat.class);

    public ExporterFactory(List<Exporter> all) {
        for (Exporter e : all) {
            exporters.put(e.format(), e);
        }
    }

    public Exporter get(ExportFormat format) {
        Exporter e = exporters.get(format);
        if (e == null) {
            throw new IllegalArgumentException("No exporter for format " + format);
        }
        return e;
    }
}
