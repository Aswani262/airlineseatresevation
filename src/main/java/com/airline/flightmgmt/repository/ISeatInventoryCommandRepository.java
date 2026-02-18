package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.SeatInventory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ISeatInventoryCommandRepository extends CrudRepository<SeatInventory, UUID> {

    List<SeatInventory> findByFlightIdAndSeatNumberIn(UUID flightId, List<String> normalized);

}