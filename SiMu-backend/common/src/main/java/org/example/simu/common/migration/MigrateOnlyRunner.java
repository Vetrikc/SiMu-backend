package org.example.simu.common.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Runs the application only far enough to apply Liquibase migrations, then exits with code 0.
 * <p>
 * Activated by {@code --app.migrate-only=true} together with
 * {@code --spring.main.web-application-type=none}; used by the Kubernetes migration Job so that
 * schema changes are applied once, before new replicas start.
 */
public class MigrateOnlyRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrateOnlyRunner.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("Migrations applied, exiting (migrate-only mode)");
        System.exit(0);
    }
}
