package org.example.simu.playlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Stored response of a POST that carried an {@code Idempotency-Key} header: replaying the request
 * returns the same body instead of creating a duplicate playlist.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String idempotencyKey, Long userId, int responseStatus, String responseBody) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getUserId() {
        return userId;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
