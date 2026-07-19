package com.paymentprocessor.analytics.service.notification;

import com.paymentprocessor.analytics.domain.entity.ReportJob;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Fans a terminal report event out to every configured channel; failures are isolated. */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final List<ReportNotifier> notifiers;

    public NotificationDispatcher(List<ReportNotifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void dispatch(ReportJob job, String downloadUrl) {
        for (ReportNotifier n : notifiers) {
            try {
                n.notify(job, downloadUrl);
            } catch (RuntimeException e) {
                log.error("Notifier {} failed for job {}", n.channel(), job.getId(), e);
            }
        }
    }
}
