package org.example.simu.auth.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.simu.auth.entity.OutboxEvent;
import org.example.simu.auth.repository.OutboxEventRepository;
import org.example.simu.common.kafka.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Drains the outbox into Kafka. Runs after commit, so consumers only ever see events of data that
 * really exists; failures keep the row unpublished and are retried on the next tick.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(KafkaTopics.USER_EVENTS, event.getAggregateId().toString(), event.getPayload());
            record.headers().add(KafkaTopics.EVENT_TYPE_HEADER,
                    event.getEventType().getBytes(StandardCharsets.UTF_8));
            try {
                kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
                log.debug("Published {} for aggregate {}", event.getEventType(), event.getAggregateId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError("publishing interrupted");
                break;
            } catch (Exception e) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(truncate(e.getMessage(), 500));
                log.warn("Kafka is not available, keeping event {} for retry: {}", event.getId(), e.getMessage());
            }
            outboxEventRepository.save(event);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
