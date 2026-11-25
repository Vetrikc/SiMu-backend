package org.example.simu.auth.kafka;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Drains the outbox into Kafka.
 * <p>
 * Rows are taken with {@code FOR UPDATE SKIP LOCKED}, so several auth replicas publish disjoint
 * batches instead of duplicating events. Sending is asynchronous and awaited once per batch, which
 * keeps the transaction short even when the broker is slow; failures leave the row unpublished and
 * bump its attempt counter.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        Gauge.builder("outbox.unpublished", outboxEventRepository, OutboxEventRepository::countByPublishedAtIsNull)
                .description("Events waiting to be published")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = outboxEventRepository.lockPending(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }

        List<CompletableFuture<Object>> futures = events.stream()
                .map(event -> kafkaTemplate.send(record(event))
                        .handle((result, error) -> {
                            if (error != null) {
                                event.setAttempts(event.getAttempts() + 1);
                                event.setLastError(truncate(error.getMessage(), 500));
                                log.warn("Kafka is not available, keeping event {} for retry: {}",
                                        event.getId(), error.getMessage());
                            } else {
                                event.setPublishedAt(Instant.now());
                                event.setLastError(null);
                            }
                            return null;
                        }))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Publishing interrupted, {} events stay unpublished", events.size());
        } catch (Exception e) {
            log.warn("Outbox batch timed out: {}", e.getMessage());
        }

        outboxEventRepository.saveAll(events);
    }

    private ProducerRecord<String, String> record(OutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaTopics.USER_EVENTS, event.getAggregateId().toString(), event.getPayload());
        record.headers().add(KafkaTopics.EVENT_TYPE_HEADER,
                event.getEventType().getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}