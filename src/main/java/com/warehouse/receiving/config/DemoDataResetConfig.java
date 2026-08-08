package com.warehouse.receiving.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.warehouse.receiving.service.DemoDataResetService;

/**
 * Wires the demo reset. Scoped to the dev profile, which is the only one the
 * public demo runs — a prod deployment holds real receiving data and must never
 * schedule a job that deletes it.
 */
@Configuration
@Profile("dev")
@EnableScheduling
@ConditionalOnProperty(name = "app.demo.reset.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataResetConfig {

    @Bean
    public DemoDataResetService demoDataResetService(DataSource dataSource) {
        return new DemoDataResetService(dataSource);
    }

    @Bean
    public DemoDataResetScheduler demoDataResetScheduler(DemoDataResetService service) {
        return new DemoDataResetScheduler(service);
    }

    /**
     * Not @Component: component scanning would create it regardless of the
     * profile and property gates above, and prod must never hold a bean whose
     * job is deleting every row.
     */
    public static class DemoDataResetScheduler {

        private static final Logger log = LoggerFactory.getLogger(DemoDataResetScheduler.class);

        private final DemoDataResetService service;

        DemoDataResetScheduler(DemoDataResetService service) {
            this.service = service;
        }

        /**
         * fixedDelay, not cron: free hosting stops the service when idle, so the
         * clock restarts on every wake. The initial delay is what matters — it
         * hands the next visitor after a cold start a clean demo.
         */
        @Scheduled(
                initialDelayString = "${app.demo.reset.initial-delay:PT5M}",
                fixedDelayString = "${app.demo.reset.interval:PT6H}")
        public void resetOnSchedule() {
            try {
                service.reset();
            } catch (RuntimeException e) {
                // Never let a failed reset kill the scheduler thread; the demo
                // staying stale is a far smaller problem than it stopping.
                log.error("Scheduled demo reset failed", e);
            }
        }
    }
}
