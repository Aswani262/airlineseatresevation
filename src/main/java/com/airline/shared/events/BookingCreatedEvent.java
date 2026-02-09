package com.airline.shared.events;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class BookingCreatedEvent extends IntegrationEvent {

    private final UUID bookingId;
    private final String bookingReference;
    private final UUID flightId;
    private final BigDecimal totalAmount;
    private final Instant holdExpiresAt;

    public BookingCreatedEvent(
            UUID bookingId,
            String bookingReference,
            UUID flightId,
            BigDecimal totalAmount,
            Instant holdExpiresAt
    ) {
        super();
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.flightId = flightId;
        this.totalAmount = totalAmount;
        this.holdExpiresAt = holdExpiresAt;
    }

    @Override
    public String getEventType() {
        return "booking.created.v1";
    }

}
