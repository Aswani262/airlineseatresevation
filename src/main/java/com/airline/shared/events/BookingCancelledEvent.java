package com.airline.shared.events;

import lombok.Getter;

import java.util.UUID;

@Getter
public final class BookingCancelledEvent extends IntegrationEvent {
    private final UUID bookingId;
    private final UUID flightId;

    public BookingCancelledEvent(
            UUID bookingId,
            UUID flightId
    ) {
        super();
        this.bookingId = bookingId;
        this.flightId = flightId;
    }

    @Override
    public String getEventType() {
        return "booking.cancelled.v1";
    }

}
