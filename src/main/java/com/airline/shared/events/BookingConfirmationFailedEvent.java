package com.airline.shared.events;

import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class BookingConfirmationFailedEvent extends  IntegrationEvent{

    private UUID flightId;
    private List<UUID> seatTemplateIds;
    private UUID customerId;

    public BookingConfirmationFailedEvent(
            UUID flightId,List<UUID> seatTemplateIds,UUID customerId
    ) {
        super();
        this.seatTemplateIds = Objects.requireNonNull(seatTemplateIds);
        this.flightId = Objects.requireNonNull(flightId);
        this.customerId = Objects.requireNonNull(customerId);
    }

    @Override
    public String getEventType() {
        return "booking.confirmation.failed.v1";
    }
}
