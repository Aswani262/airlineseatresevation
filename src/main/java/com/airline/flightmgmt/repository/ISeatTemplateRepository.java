package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.SeatTemplate;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;


public interface ISeatTemplateRepository extends CrudRepository<SeatTemplate, UUID> {
    List<SeatTemplate> findByAircraftId(UUID aircraftId);

}
