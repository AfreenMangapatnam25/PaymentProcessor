package com.paymentprocessor.analytics.service.template;

import com.paymentprocessor.analytics.domain.enums.ReportType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Resolves a {@link ReportType} to its {@link ReportTemplate}. */
@Component
public class TemplateRegistry {

    private final Map<ReportType, ReportTemplate> templates = new EnumMap<>(ReportType.class);

    public TemplateRegistry(List<ReportTemplate> all) {
        for (ReportTemplate t : all) {
            templates.put(t.type(), t);
        }
    }

    public boolean supports(ReportType type) { return templates.containsKey(type); }

    public ReportTemplate get(ReportType type) {
        ReportTemplate t = templates.get(type);
        if (t == null) {
            throw new IllegalArgumentException("No template for report type " + type);
        }
        return t;
    }

    public Set<ReportType> supportedTypes() { return templates.keySet(); }
}
