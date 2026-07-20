package com.paymentprocessor.analytics.service.notification;

import com.paymentprocessor.analytics.config.AnalyticsProperties;
import com.paymentprocessor.analytics.domain.entity.ReportJob;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes a report.completed event to Kafka for downstream consumers/webhooks. */
@Component
public class KafkaReportNotifier implements ReportNotifier {

    private static final Logger log = LoggerFactory.getLogger(KafkaReportNotifier.class);

    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public KafkaReportNotifier(KafkaTemplate<String, Object> kafka, AnalyticsProperties props) {
        this.kafka = kafka;
        this.topic = props.getNotifications().getKafkaTopic();
    }

    @Override
    public void notify(ReportJob job, String downloadUrl) {
        ReportCompletedEvent event = new ReportCompletedEvent(
                job.getId(), job.getMerchantId(), job.getReportType(), job.getFormat(),
                job.getStatus(), job.getRowCount(), job.getSizeBytes(), job.getStorageKey(),
                downloadUrl, job.getErrorMessage(), Instant.now());
        // key by merchant so a merchant's report events stay ordered on one partition
        kafka.send(topic, job.getMerchantId(), event).whenComplete((r, ex) -> {
            if (ex != null) {
                log.error("Failed to publish report event for job {}", job.getId(), ex);
            } else {
                log.debug("Published report event for job {} to {}", job.getId(), topic);
            }
        });
    }

    @Override
    public String channel() { return "kafka"; }
}
