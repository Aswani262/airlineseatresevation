package com.airline.flightmgmt.domain;

import java.util.UUID;

public record SeatKey(UUID flightId, UUID customerId) {

    @Override
    public String toString() {
        return flightId + ":" + customerId;
    }
}