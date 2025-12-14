package com.itmentorcommunityplatform.dataimporter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaStartupValidator {

    private final KafkaProperties kafkaProperties;
    private static final String REQUIRED_TOPIC = "projects.project.created";

    @EventListener(ApplicationReadyEvent.class)
    public void validateKafkaConnection() {
        log.info("Validating Kafka connection and topic existence: {}", REQUIRED_TOPIC);
        try (AdminClient client = AdminClient.create(kafkaProperties.buildAdminProperties(null))) {
            ListTopicsOptions options = new ListTopicsOptions();
            options.timeoutMs(5000);
            Set<String> topics = client.listTopics(options).names().get(5, TimeUnit.SECONDS);
            if (!topics.contains(REQUIRED_TOPIC)) {
                log.error("CRITICAL: Required Kafka topic '{}' is missing! Available topics: {}", REQUIRED_TOPIC, topics);
                throw new IllegalStateException("Required Kafka topic missing: " + REQUIRED_TOPIC);
            }
            log.info("Kafka validation successful. Topic '{}' exists.", REQUIRED_TOPIC);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("CRITICAL: Could not connect to Kafka or timeout occurred during validation.", e);
            throw new IllegalStateException("Could not connect to Kafka broker", e);
        }
    }
}