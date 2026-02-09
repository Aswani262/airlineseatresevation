package com.airline.booking.api.dto;

import java.util.List;
import java.util.UUID;

public record ConfirmBookingResult(
        UUID bookingId,
        String bookingReference,
        String status,
        List<TicketIssued> tickets
) {
    public record TicketIssued(UUID passengerId, String ticketNumber) {}
}
