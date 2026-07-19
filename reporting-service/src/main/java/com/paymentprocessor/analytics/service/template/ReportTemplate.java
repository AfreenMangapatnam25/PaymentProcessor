package com.paymentprocessor.analytics.service.template;

import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.service.report.ReportData;

/** A named, parameterized merchant report backed by ClickHouse aggregations. */
public interface ReportTemplate {
    ReportType type();
    ReportData generate(ReportParameters params);
}
