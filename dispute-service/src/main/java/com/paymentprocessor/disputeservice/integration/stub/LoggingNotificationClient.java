package com.paymentprocessor.disputeservice.integration.stub;

import com.paymentprocessor.disputeservice.domain.enums.NotificationChannel;
import com.paymentprocessor.disputeservice.domain.enums.NotificationUrgency;
import com.paymentprocessor.disputeservice.integration.NotificationClient;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub for {@link NotificationClient} that logs merchant notifications
 * instead of dispatching them.
 *
 * <p>notification-service does exist and exposes {@code POST /api/messages},
 * but that endpoint requires a resolved {@code recipient} address (raw email
 * or phone) plus a {@code templateKey}/{@code locale} for server-side
 * rendering. dispute-service only has a merchant id (not a contact address)
 * and pre-rendered free-text subject/body, so there is no faithful mapping
 * from this interface's signature onto that endpoint without inventing a
 * template and fabricating a contact address. This intentionally remains a
 * logging fallback rather than making a call with made-up data.
 */
@Component
public class LoggingNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationClient.class);

    @Override
    public void notifyMerchant(String merchantId, String disputeId, String subject,
                               String body, NotificationUrgency urgency,
                               Set<NotificationChannel> channels) {
        log.info("[NOTIFY] merchant={} dispute={} urgency={} channels={} subject='{}'",
                merchantId, disputeId, urgency, channels, subject);
        log.debug("[NOTIFY] body: {}", body);
    }
}
