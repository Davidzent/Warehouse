package com.warehouse.receiving.service;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Restores the public demo to its seeded state.
 *
 * <p>Receiving is cumulative and irreversible by design, so ordinary use walks
 * the demo towards every PO being CLOSED — no abuse required. Rate limits slow
 * that down; only replaying the seed undoes it.
 */
public class DemoDataResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataResetService.class);

    private static final Resource RESET = new ClassPathResource("db/demo-reset.sql");
    private static final Resource SEED = new ClassPathResource("data.sql");

    private final DataSource dataSource;

    public DemoDataResetService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Both scripts run on one connection in a single transaction: a delete that
     * committed without its reseed would leave the demo empty rather than stale.
     */
    public void reset() {
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScriptUtils.executeSqlScript(connection, RESET);
                ScriptUtils.executeSqlScript(connection, SEED);
                connection.commit();
                log.info("Demo data reset to seed state");
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Demo data reset failed", e);
        }
    }
}
