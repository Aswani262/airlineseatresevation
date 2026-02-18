package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.exception.SeatNotAvailableException;
import com.airline.flightmgmt.domain.SeatInventory;
import com.airline.flightmgmt.exception.SeatLockingFailedException;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.model.SeatLockResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

// This class is responsible for handling the command to reserve a seat on a flight.
// It will interact with the domain layer to perform the necessary operations to reserve a seat,
// such as checking availability, locking the seat, and confirming the reservation.
@ApplicationService
@RequiredArgsConstructor
public class LockSeatHandler implements LockSeatUseCase {

    private final ISeatInventoryCommandRepository seatInventoryRepository;
    private final ISeatInventoryService seatInventoryService;


    @Override
    @Transactional
    public SeatLockResult lockSeat(LockSeatCommand command) {

        List<String> normalizeSeats = seatInventoryService.normalizeSeats(command.seatNumber());

        List<SeatInventory> seats = seatInventoryRepository.findByFlightIdAndSeatNumberIn(command.flightId(), normalizeSeats);

        // Lock seats - this will set the lock expiration time on the seat inventory records
        var lockResult = seatInventoryService.lockSeats(seats, command.bookingId(), Duration.ofMinutes(command.holdMinutes()));
        if (!lockResult.success()) {
            throw new SeatNotAvailableException(command.seatNumber());
        }

        //Race condition handle
        try {
            seatInventoryRepository.saveAll(seats);
        } catch (OptimisticLockingFailureException e) {
            throw new SeatLockingFailedException(seats.toString());
        }
      return lockResult;
    }
}
