package com.paymentprocessor.authorization.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the domain-event topic owned by this service. Topics are created on startup when the
 * broker permits auto topic creation via AdminClient.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String SERVICE_EVENTS_TOPIC = "authorizationservicetopic";

    @Bean
    public NewTopic authorizationServiceEventsTopic(
            @Value("${authorization.events.topic:" + SERVICE_EVENTS_TOPIC + "}") String topic) {
        return TopicBuilder.name(topic).partitions(6).replicas(1).build();
    }
}
