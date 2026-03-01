package com.airline.flightmgmt.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SeatHoldInfo {
    private final UUID customerId;
    private final UUID bookingId;        // null until confirmed
    private final HoldStage holdStage;
    private final OffsetDateTime expiresAt;
}