package com.paymentprocessor.authorization.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the domain-event topics owned by this service. Topics are created on startup when the
 * broker permits auto topic creation via AdminClient.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String AUTHORIZATION_EVENTS = "authorization.events";
    public static final String ACCESS_CONTROL_EVENTS = "authorization.access.events";

    @Bean
    public NewTopic authorizationEventsTopic() {
        return TopicBuilder.name(AUTHORIZATION_EVENTS).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic accessControlEventsTopic() {
        return TopicBuilder.name(ACCESS_CONTROL_EVENTS).partitions(3).replicas(1).build();
    }
}
