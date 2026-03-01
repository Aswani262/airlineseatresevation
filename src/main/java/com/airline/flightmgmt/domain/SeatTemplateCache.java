package com.airline.flightmgmt.domain;

import lombok.Data;

import java.util.UUID;

@Data
public class SeatTemplateCache {

    private UUID seatTemplateId;
    private String seatNumber;
    private SeatType seatType;
    private FareClass fareClass;
    private Integer rowNumber;
    private boolean isBlocked;
}
