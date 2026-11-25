package org.example.simu.playlist.repository;

import org.example.simu.playlist.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    long deleteByCreatedAtBefore(Instant threshold);
}
