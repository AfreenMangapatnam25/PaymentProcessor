package com.paymentprocessor.analytics.service.notification;

import com.paymentprocessor.analytics.domain.entity.ReportJob;

/** A single delivery channel for report lifecycle notifications. */
public interface ReportNotifier {
    void notify(ReportJob job, String downloadUrl);
    String channel();
}
