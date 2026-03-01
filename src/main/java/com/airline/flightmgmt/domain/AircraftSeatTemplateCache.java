package com.airline.flightmgmt.domain;

import lombok.Data;

import java.util.List;

@Data
public class AircraftSeatTemplateCache {
    private List<SeatTemplateCache> seatsCache;
}
