package com.airline.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class AuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(Clock.systemUTC())); // Use UTC or a specific zone if needed, e.g., OffsetDateTime.now(ZoneOffset.UTC)
    }
}