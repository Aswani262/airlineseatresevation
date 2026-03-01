package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.AircraftSeatTemplateCache;
import com.airline.flightmgmt.domain.SeatTemplate;

import java.util.List;
import java.util.UUID;

public interface ISeatTemplateCacheRepository {
    AircraftSeatTemplateCache getSeatTemplate(UUID aircraftId);
}
