package org.example.simu.user.entity;

import org.example.simu.common.jpa.AuditableEntity;
import org.hibernate.annotations.SQLRestriction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.example.simu.common.uuid.UuidV7;

import java.util.UUID;

/**
 * Local read model of an account, kept in sync through Kafka.
 * <p>
 * {@code userId} is the identifier owned by auth-service; the surrogate primary key is a UUID v7 so
 * the projection can be rebuilt/partitioned without depending on the source sequence.
 */
@Entity
@Table(name = "user_profiles")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
public class UserProfile extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @PrePersist
    void ensureId() {
        if (id == null) {
            id = UuidV7.generate();
        }
    }
}
