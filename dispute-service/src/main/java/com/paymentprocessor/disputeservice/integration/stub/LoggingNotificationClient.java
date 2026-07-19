package com.paymentprocessor.disputeservice.integration.stub;

import com.paymentprocessor.disputeservice.domain.enums.NotificationChannel;
import com.paymentprocessor.disputeservice.domain.enums.NotificationUrgency;
import com.paymentprocessor.disputeservice.integration.NotificationClient;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development stub for {@link NotificationClient} that logs merchant
 * notifications instead of dispatching them.
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
