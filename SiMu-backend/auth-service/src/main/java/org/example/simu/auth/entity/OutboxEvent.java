package org.example.simu.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.example.simu.common.uuid.UuidV7;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox: the event row is written in the same transaction as the user change, so a
 * committed user change can never lose its Kafka event (and vice versa). A scheduled publisher
 * drains the table into Kafka.
 * <p>
 * The primary key is a UUID v7 generated on insert: strictly time-ordered, so the index stays
 * append-only and the publisher can read the backlog in insertion order.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UuidV7.generate();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
