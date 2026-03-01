package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.Flight;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface IFlightCommandRepository extends CrudRepository<Flight, UUID> {

    @Query("SELECT f.flightDate FROM Flight f WHERE f.flightId = :flightId")
    Optional<LocalDate> getFlightDateById(@Param("flightId") UUID flightId);
}
