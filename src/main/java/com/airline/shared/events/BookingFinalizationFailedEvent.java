package com.airline.shared.events;

import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class BookingFinalizationFailedEvent extends IntegrationEvent{

    private UUID flightId;
    private final List<UUID> seatTemplateIds;
    private final UUID customerId;
    private final UUID bookingId;

    public BookingFinalizationFailedEvent(UUID flightId, List<UUID> seatTemplateIds, UUID customerId,UUID bookingId) {
        super();
        this.seatTemplateIds = Objects.requireNonNull(seatTemplateIds);
        this.flightId = Objects.requireNonNull(flightId);
        this.customerId = Objects.requireNonNull(customerId);
        this.bookingId = Objects.requireNonNull(bookingId);

    }

    @Override
    public String getEventType() {
        return "released.lock.v1";
    }

}
