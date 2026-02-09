package com.airline.booking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(TicketJdbcRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class TicketJdbcRepositoryTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TicketJdbcRepository repo;

    @BeforeEach
    void setupSchema() {
        jdbcTemplate.execute("""
            DROP TABLE IF EXISTS tickets;

            CREATE TABLE tickets (
                id UUID PRIMARY KEY,
                ticket_number VARCHAR(50) NOT NULL,
                booking_id UUID NOT NULL,
                passenger_id UUID NOT NULL,
                status VARCHAR(20) NOT NULL
            );
        """);
    }

    @Test
    void insertTickets_shouldInsertAllRows() {
        UUID bookingId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        var t1 = new TicketRepository.TicketRow(
                UUID.randomUUID(),
                "TKT-001",
                bookingId,
                p1,
                "ISSUED"
        );

        var t2 = new TicketRepository.TicketRow(
                UUID.randomUUID(),
                "TKT-002",
                bookingId,
                p2,
                "ISSUED"
        );

        repo.insertTickets(List.of(t1, t2));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tickets WHERE booking_id = ?",
                Integer.class,
                bookingId
        );
        assertThat(count).isEqualTo(2);

        Map<String, Object> row1 = jdbcTemplate.queryForMap(
                "SELECT * FROM tickets WHERE id = ?",
                t1.id()
        );

        assertThat(row1.get("ticket_number")).isEqualTo("TKT-001");
        assertThat(row1.get("booking_id")).isEqualTo(bookingId);
        assertThat(row1.get("passenger_id")).isEqualTo(p1);
        assertThat(row1.get("status")).isEqualTo("ISSUED");
    }

    @Test
    void cancelTicketsByBooking_shouldCancelOnlyIssuedTicketsForThatBooking() {
        UUID bookingA = UUID.randomUUID();
        UUID bookingB = UUID.randomUUID();

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();

        // Booking A: ISSUED + REFUNDED
        jdbcTemplate.update("""
            INSERT INTO tickets (id, ticket_number, booking_id, passenger_id, status)
            VALUES (?, ?, ?, ?, ?)
        """, UUID.randomUUID(), "A-1", bookingA, p1, "ISSUED");

        jdbcTemplate.update("""
            INSERT INTO tickets (id, ticket_number, booking_id, passenger_id, status)
            VALUES (?, ?, ?, ?, ?)
        """, UUID.randomUUID(), "A-2", bookingA, p2, "REFUNDED");

        // Booking B: ISSUED
        jdbcTemplate.update("""
            INSERT INTO tickets (id, ticket_number, booking_id, passenger_id, status)
            VALUES (?, ?, ?, ?, ?)
        """, UUID.randomUUID(), "B-1", bookingB, p3, "ISSUED");

        // Act
        repo.cancelTicketsByBooking(bookingA);

        // Assert: bookingA ISSUED -> CANCELLED
        Integer cancelledA = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM tickets
            WHERE booking_id = ? AND status = 'CANCELLED'
        """, Integer.class, bookingA);
        assertThat(cancelledA).isEqualTo(1);

        // Assert: bookingA REFUNDED untouched
        Integer refundedA = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM tickets
            WHERE booking_id = ? AND status = 'REFUNDED'
        """, Integer.class, bookingA);
        assertThat(refundedA).isEqualTo(1);

        // Assert: bookingB untouched
        Integer issuedB = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM tickets
            WHERE booking_id = ? AND status = 'ISSUED'
        """, Integer.class, bookingB);
        assertThat(issuedB).isEqualTo(1);
    }

    @Test
    void cancelTicketsByBooking_shouldDoNothing_whenNoIssuedTickets() {
        UUID bookingId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO tickets (id, ticket_number, booking_id, passenger_id, status)
            VALUES (?, ?, ?, ?, ?)
        """, UUID.randomUUID(), "X-1", bookingId, UUID.randomUUID(), "REFUNDED");

        repo.cancelTicketsByBooking(bookingId);

        Integer cancelled = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM tickets
            WHERE booking_id = ? AND status = 'CANCELLED'
        """, Integer.class, bookingId);

        assertThat(cancelled).isEqualTo(0);
    }
}
