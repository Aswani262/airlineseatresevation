package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.domain.SeatStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ISeatInventoryCommandRepository extends CrudRepository<SeatAssignments, UUID> {

    Optional<SeatAssignments> findByFlightIdAndSeatTemplateIdAndFlightDate(UUID flightId, UUID templateId, LocalDate flightDate);

    List<SeatAssignments> findByFlightIdAndCustomerIdAndStatusAndFlightDate(UUID flightId, UUID customerId, SeatStatus seatStatus, LocalDate flightDate);

    List<SeatAssignments> findByBookingIdAndFlightDate(UUID bookingId, LocalDate flightDate);

    List<SeatAssignments> findByStatusAndLockExpiresAtBefore(SeatStatus seatStatus, OffsetDateTime now);
}