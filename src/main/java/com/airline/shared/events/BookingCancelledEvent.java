package com.airline.shared.events;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public final class BookingCancelledEvent extends IntegrationEvent {

    private final UUID bookingId;
    private final String bookingReference;
    private final UUID flightId;
    private final String reason;
    private final Instant cancelledAt;

    public BookingCancelledEvent(
            UUID bookingId,
            String bookingReference,
            UUID flightId,
            String reason,
            Instant cancelledAt
    ) {
        super();
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.flightId = flightId;
        this.reason = reason;
        this.cancelledAt = cancelledAt;
    }

    @Override
    public String getEventType() {
        return "booking.cancelled.v1";
    }

}
