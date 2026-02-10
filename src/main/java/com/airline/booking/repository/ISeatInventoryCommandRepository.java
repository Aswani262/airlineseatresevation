package com.airline.booking.repository;

import com.airline.booking.domain.model.SeatInventory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ISeatInventoryCommandRepository extends CrudRepository<SeatInventory, UUID> {

    List<SeatInventory> findByFlightIdAndSeatNumberIn(UUID flightId, List<String> normalized);

}