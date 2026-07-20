package com.paymentprocessor.analytics.service.template;

import java.time.LocalDate;
import java.util.Map;

/** Inputs common to every merchant report template. */
public record ReportParameters(
        String merchantId,
        LocalDate fromDate,
        LocalDate toDate,
        Map<String, Object> options) {

    public Object option(String key) { return options == null ? null : options.get(key); }
}
