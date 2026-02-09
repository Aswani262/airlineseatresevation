package com.airline.shared.events;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public final class BookingExpiredEvent extends IntegrationEvent {

    private final UUID bookingId;
    private final String bookingReference;
    private final UUID flightId;
    private final Instant expiredAt;

    public BookingExpiredEvent(
            UUID bookingId,
            String bookingReference,
            UUID flightId,
            Instant expiredAt
    ) {
        super();
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.flightId = flightId;
        this.expiredAt = expiredAt;
    }

    @Override
    public String getEventType() {
        return "booking.expired.v1";
    }

}
