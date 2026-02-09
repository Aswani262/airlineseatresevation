package com.airline.shared.events;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public final class BookingConfirmedEvent extends IntegrationEvent {

    private final UUID bookingId;
    private final String bookingReference;
    private final UUID flightId;
    private final UUID paymentId;
    private final BigDecimal totalAmount;
    private final Instant confirmedAt;

    public BookingConfirmedEvent(
            UUID bookingId,
            String bookingReference,
            UUID flightId,
            UUID paymentId,
            BigDecimal totalAmount,
            Instant confirmedAt
    ) {
        super(); // sets eventId + occurredOn
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.flightId = flightId;
        this.paymentId = paymentId;
        this.totalAmount = totalAmount;
        this.confirmedAt = confirmedAt;
    }

    @Override
    public String getEventType() {
        return "booking.confirmed.v1";
    }

}
