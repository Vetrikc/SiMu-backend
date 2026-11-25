package org.example.simu.playlist.security;

import org.example.simu.playlist.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Keys live for a day: enough to protect against client retries, small enough to stay bounded. */
@Component
public class IdempotencyCleanup {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanup.class);

    private final IdempotencyKeyRepository repository;

    public IdempotencyCleanup(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        long removed = repository.deleteByCreatedAtBefore(Instant.now().minus(Duration.ofHours(24)));
        if (removed > 0) {
            log.info("Purged {} expired idempotency keys", removed);
        }
    }
}
