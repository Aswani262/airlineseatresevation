package com.airline.shared.events;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredOn;

    protected DomainEvent() {
        this(UUID.randomUUID(), Instant.now());
    }

    protected DomainEvent(UUID eventId, Instant occurredOn) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    /** Stable identifier used by consumers/routing */
    public abstract String getEventType();
}
