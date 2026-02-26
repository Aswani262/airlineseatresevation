package com.airline.shared.events;

import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class ReleaseHoldSeatEvent extends IntegrationEvent{

    private UUID flightId;
    private final List<UUID> seatTemplateIds;

    public ReleaseHoldSeatEvent(UUID flightId, List<UUID> seatTemplateIds) {
        super();
        this.seatTemplateIds = Objects.requireNonNull(seatTemplateIds);
        this.flightId = Objects.requireNonNull(flightId);

    }

    @Override
    public String getEventType() {
        return "released.lock.v1";
    }

}
