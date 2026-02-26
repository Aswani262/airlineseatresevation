package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.SeatAssignments;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ISeatInventoryCommandRepository extends CrudRepository<SeatAssignments, UUID> {


    Optional<SeatAssignments> findByFlightIdAndSeatTemplateId(UUID flightId, UUID templateId);

    List<SeatAssignments> findByFlightIdAndTemplateIdIn(UUID flightId, List<UUID> seatTemplateIds);
}