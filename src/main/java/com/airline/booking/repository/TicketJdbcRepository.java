package com.airline.booking.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class TicketJdbcRepository implements TicketRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TicketJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertTickets(List<TicketRow> tickets) {
        String sql = """
            INSERT INTO tickets (id, ticket_number, booking_id, passenger_id, status)
            VALUES (:id, :ticketNumber, :bookingId, :passengerId, :status)
        """;

        var batchParams = tickets.stream()
                .map(t -> new MapSqlParameterSource()
                        .addValue("id", t.id())
                        .addValue("ticketNumber", t.ticketNumber())
                        .addValue("bookingId", t.bookingId())
                        .addValue("passengerId", t.passengerId())
                        .addValue("status", t.status())
                )
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    @Override
    public void cancelTicketsByBooking(UUID bookingId) {
        String sql = """
            UPDATE tickets
            SET status = 'CANCELLED'
            WHERE booking_id = :bookingId
              AND status = 'ISSUED'
        """;

        var params = new MapSqlParameterSource()
                .addValue("bookingId", bookingId);

        jdbcTemplate.update(sql, params);
    }


//    INSERT INTO tickets (id, ticket_number, booking_id, passenger_id, status)
//    VALUES (:id, :ticketNumber, :bookingId, :passengerId, :status)
//    ON CONFLICT (id) DO NOTHING;
}
