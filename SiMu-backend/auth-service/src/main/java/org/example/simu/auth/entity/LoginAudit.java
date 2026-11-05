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
 * Append-only log of authentication attempts (successful and failed).
 */
@Entity
@Table(name = "login_audit")
@Getter
@Setter
public class LoginAudit {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    /** PASSWORD or GOOGLE - lets an anomaly detector spot a changed sign-in method. */
    @Column(name = "auth_method", length = 20)
    private String authMethod;

    /** SHA-256 of the provider subject: correlatable, but not stored in clear text. */
    @Column(name = "subject_hash", length = 64)
    private String subjectHash;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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