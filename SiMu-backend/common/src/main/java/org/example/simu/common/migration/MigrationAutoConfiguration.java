package org.example.simu.common.migration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link MigrateOnlyRunner} only when the application is started as a migration job.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "app.migrate-only", havingValue = "true")
public class MigrationAutoConfiguration {

    @Bean
    public MigrateOnlyRunner migrateOnlyRunner() {
        return new MigrateOnlyRunner();
    }
}
