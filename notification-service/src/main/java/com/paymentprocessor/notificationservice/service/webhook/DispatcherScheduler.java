package com.paymentprocessor.notificationservice.service.webhook;

import com.paymentprocessor.notificationservice.config.DispatcherProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Schedules WebhookDispatcher#dispatchBatch() at notification.dispatcher.poll-interval.
 *
 * Deliberately programmatic (TaskScheduler#scheduleWithFixedDelay) rather
 * than @Scheduled(fixedDelayString = "#{...}"): a bean produced via
 * @ConfigurationPropertiesScan is registered under a generated name (prefix
 * + fully-qualified class name), not the simple "dispatcherProperties" a
 * SpEL #{beanName...} reference would need -- so that annotation-based
 * approach would fail to resolve at startup. Plain constructor injection of
 * DispatcherProperties below is unaffected, since that's resolved by type.
 */
@Component
public class DispatcherScheduler {

    private static final Logger log = LoggerFactory.getLogger(DispatcherScheduler.class);

    private final WebhookDispatcher dispatcher;
    private final DispatcherProperties properties;
    private final TaskScheduler taskScheduler;

    public DispatcherScheduler(WebhookDispatcher dispatcher, DispatcherProperties properties, TaskScheduler taskScheduler) {
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    void schedule() {
        taskScheduler.scheduleWithFixedDelay(this::poll, properties.getPollInterval());
    }

    void poll() {
        int attempted = dispatcher.dispatchBatch();
        if (attempted > 0) {
            log.debug("dispatcher poll attempted {} delivery(ies)", attempted);
        }
    }
}
