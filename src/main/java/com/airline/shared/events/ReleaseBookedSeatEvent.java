package com.airline.shared.events;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ReleaseBookedSeatEvent extends IntegrationEvent{

    private UUID flightId;
    private final List<UUID> seatTemplateIds;

    public ReleaseBookedSeatEvent(UUID flightId, List<UUID> seatTemplateIds) {
        super();
        this.seatTemplateIds = Objects.requireNonNull(seatTemplateIds);
        this.flightId = Objects.requireNonNull(flightId);

    }

    @Override
    public String getEventType() {
        return "release.booked.seat.v1";
    }
}
