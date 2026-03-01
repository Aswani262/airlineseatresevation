package com.airline.flightmgmt.domain;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;
//Only hold data which required in cache
// Adding the whole entity in cache is not require
// and its increase the memmory
@Data
public class FlightCache {
    private UUID flightId;
    private LocalDate flightDate;
    private UUID aircraftId;
}
