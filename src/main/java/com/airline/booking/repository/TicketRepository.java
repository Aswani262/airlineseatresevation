package com.airline.booking.repository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository {
    void insertTickets(List<TicketRow> tickets);
    void cancelTicketsByBooking(UUID bookingId);
    record TicketRow(UUID id, String ticketNumber, UUID bookingId, UUID passengerId, String status) {}
}
