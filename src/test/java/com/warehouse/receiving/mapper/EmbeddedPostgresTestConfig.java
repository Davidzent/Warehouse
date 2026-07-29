package com.warehouse.receiving.mapper;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

@TestConfiguration
public class EmbeddedPostgresTestConfig {

    private static volatile EmbeddedPostgres embedded;

    private static EmbeddedPostgres instance() {
        if (embedded == null) {
            synchronized (EmbeddedPostgresTestConfig.class) {
                if (embedded == null) {
                    try {
                        embedded = EmbeddedPostgres.builder().start();
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
                    }
                }
            }
        }
        return embedded;
    }

    @Bean
    public DataSource dataSource() {
        return instance().getPostgresDatabase();
    }
}
