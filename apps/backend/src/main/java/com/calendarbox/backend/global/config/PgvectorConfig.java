package com.calendarbox.backend.global.config;

import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.sql.Connection;


@Slf4j
@Configuration
public class PgvectorConfig {

    @Bean
    public Object registerPgvectorTypes(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            PGvector.registerTypes(conn);
            log.info("[pgvector] registered types on startup");
        } catch (Exception e) {
            log.error("[pgvector] failed to register types", e);
        }
        return new Object();
    }
}
