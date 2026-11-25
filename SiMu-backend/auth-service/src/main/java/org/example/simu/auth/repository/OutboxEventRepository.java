package org.example.simu.auth.repository;

import org.example.simu.auth.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Locks a batch of unpublished rows so that several auth replicas never publish the same event
     * twice. SKIP LOCKED makes the replicas take disjoint batches instead of waiting for each other.
     */
    @Query(value = "select * from outbox_events where published_at is null " +
            "order by created_at asc limit :limit for update skip locked", nativeQuery = true)
    List<OutboxEvent> lockPending(@Param("limit") int limit);

    long countByPublishedAtIsNull();

    /** Uses the partial index on {@code published_at IS NULL}. */
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}