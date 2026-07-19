package com.paymentprocessor.authenticationservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Idempotent, acks=all String producer used by the outbox relay. The outbox stores
 * events as JSON text, so we ship them verbatim rather than re-serializing.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, String> outboxProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        cfg.put(ProducerConfig.ACKS_CONFIG, "all");
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        cfg.put(ProducerConfig.RETRIES_CONFIG, 3);
        cfg.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate(
            ProducerFactory<String, String> outboxProducerFactory) {
        return new KafkaTemplate<>(outboxProducerFactory);
    }
}
