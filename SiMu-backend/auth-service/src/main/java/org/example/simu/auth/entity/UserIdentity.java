package org.example.simu.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.example.simu.common.uuid.UuidV7;

import java.time.Instant;
import java.util.UUID;

/**
 * Link between a local account and an external identity provider.
 * <p>
 * The pair {@code (provider, subject)} is the only stable key: Google's {@code sub} never changes,
 * while the e-mail of an account may change over time. A unique constraint on the pair also makes
 * find-or-create safe under concurrent callbacks.
 */
@Entity
@Table(name = "user_identities")
@Getter
@Setter
public class UserIdentity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "email")
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UuidV7.generate();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
