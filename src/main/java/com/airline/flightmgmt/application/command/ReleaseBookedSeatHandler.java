package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.ReleaseBookedSeatCommand;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.repository.IFlightCacheRepository;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.core.ExpiringSeatHoldManager;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class ReleaseBookedSeatHandler implements ReleaseBookedSeatUseCase {
    private final ISeatInventoryCommandRepository repository;
    private final ExpiringSeatHoldManager expiringSeatHoldManager;
    private final IFlightCacheRepository flightCacheRepository;

    @Transactional
    @Override
    public void releaseBookedSeats(ReleaseBookedSeatCommand command) {

        UUID flightId = command.flightId();

        LocalDate flightDate = flightCacheRepository.getFlight(flightId).getFlightDate();

        //Release all seat associated with this booking Id
        List<SeatAssignments>  seatsToRelease = repository.findByBookingIdAndFlightDate(command.bookingId(),flightDate);

        //Release means here is to delete from seat assigment as seat assigment only contains hold/booked status
        //Those seat are not in seat assigment is consider as available , because we are generating seat availability
        // dynamically
        repository.deleteAll(seatsToRelease);

        expiringSeatHoldManager.removeHold(command.flightId(),command.customerId());

    }

}