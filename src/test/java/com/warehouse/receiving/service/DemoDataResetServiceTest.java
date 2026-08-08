package com.warehouse.receiving.service;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import com.warehouse.receiving.mapper.EmbeddedPostgresTestConfig;

/**
 * Deliberately no Spring context. The service takes its own connection, which a
 * @MybatisTest's rolled-back transaction would hide the schema from — the point
 * here is that the reset commits.
 */
class DemoDataResetServiceTest {

    private static final DataSource DATA_SOURCE = new EmbeddedPostgresTestConfig().dataSource();

    private JdbcTemplate jdbc;
    private DemoDataResetService service;

    @BeforeEach
    void seedFromScratch() throws SQLException {
        jdbc = new JdbcTemplate(DATA_SOURCE);
        service = new DemoDataResetService(DATA_SOURCE);
        try (Connection connection = DATA_SOURCE.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private int receivedOnLine(long poLineId) {
        return jdbc.queryForObject(
                "SELECT quantity_received FROM purchase_order_line WHERE po_line_id = ?",
                Integer.class, poLineId);
    }

    private String statusOf(long poId) {
        return jdbc.queryForObject(
                "SELECT status FROM purchase_order WHERE po_id = ?", String.class, poId);
    }

    /** Stands in for a visitor receiving PO 1000 to completion. */
    private void simulateReceiving() {
        jdbc.update("INSERT INTO receipt (po_id, received_by, received_at) VALUES (1000, 'visitor', now())");
        jdbc.update("UPDATE purchase_order_line SET quantity_received = 50 WHERE po_line_id = 2000");
        jdbc.update("UPDATE purchase_order SET status = 'CLOSED' WHERE po_id = 1000");
    }

    @Test
    void undoesReceivingAndReturnsThePurchaseOrderToItsSeededState() {
        simulateReceiving();
        assertThat(statusOf(1000L)).isEqualTo("CLOSED");
        assertThat(receivedOnLine(2000L)).isEqualTo(50);

        service.reset();

        assertThat(statusOf(1000L)).isEqualTo("OPEN");
        assertThat(receivedOnLine(2000L)).isZero();
    }

    @Test
    void clearsReceiptsAddedSinceTheLastReset() {
        int seededReceipts = count("receipt");
        simulateReceiving();
        assertThat(count("receipt")).isEqualTo(seededReceipts + 1);

        service.reset();

        assertThat(count("receipt")).isEqualTo(seededReceipts);
    }

    @Test
    void restoresEveryDemoTableToItsSeededRowCount() {
        String[] tables = {
            "vendor", "product", "location", "purchase_order",
            "purchase_order_line", "receipt", "receipt_line", "inventory",
        };
        int[] seeded = new int[tables.length];
        for (int i = 0; i < tables.length; i++) seeded[i] = count(tables[i]);

        simulateReceiving();
        service.reset();

        for (int i = 0; i < tables.length; i++) {
            assertThat(count(tables[i])).as(tables[i]).isEqualTo(seeded[i]);
        }
    }

    /** The scheduler runs this repeatedly; a second pass must not trip a key clash. */
    @Test
    void isIdempotentAcrossRepeatedRuns() {
        service.reset();
        service.reset();
        service.reset();

        assertThat(statusOf(1000L)).isEqualTo("OPEN");
        assertThat(receivedOnLine(2000L)).isZero();
    }

    /** data.sql moves the sequences past the seeded ids, so new rows do not collide. */
    @Test
    void leavesSequencesUsableForNewRows() {
        service.reset();

        jdbc.update("INSERT INTO receipt (po_id, received_by, received_at) VALUES (1000, 'after-reset', now())");

        Long id = jdbc.queryForObject(
                "SELECT receipt_id FROM receipt WHERE received_by = 'after-reset'", Long.class);
        assertThat(id).isGreaterThan(5000L);
    }
}
