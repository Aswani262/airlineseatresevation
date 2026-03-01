package com.airline.shared.events;

import lombok.Getter;

import java.util.UUID;

@Getter
public final class BookingConfirmedEvent extends IntegrationEvent {

    private final UUID bookingId;

    public BookingConfirmedEvent(
            UUID bookingId
    ) {
        super(); // sets eventId + occurredOn
        this.bookingId = bookingId;
    }

    @Override
    public String getEventType() {
        return "booking.confirmed.v1";
    }

}
