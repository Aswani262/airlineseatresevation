package com.airline.flightmgmt.application.command;

import com.airline.booking.exception.SeatNotFound;
import com.airline.flightmgmt.api.dto.SeatBookedResult;
import com.airline.flightmgmt.application.command.dto.ConfirmSeatCommand;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.repository.IFlightCacheRepository;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.core.ExpiringSeatHoldManager;
import com.airline.flightmgmt.service.core.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class BookSeatHandler implements BookSeatUseCase {

    private final ISeatInventoryCommandRepository seatInventoryRepository;
    private final ISeatInventoryService seatInventoryService;
    private final IFlightCacheRepository flightCacheRepository;
    private final ExpiringSeatHoldManager expiringSeatHoldManager;

    @Override
    @Transactional
    public SeatBookedResult bookHoldSeat(ConfirmSeatCommand command) {

        UUID flightId = command.flightId();

        LocalDate flightDate = flightCacheRepository.getFlight(flightId).getFlightDate();

        List<SeatAssignments> seatsToBeBooked = new ArrayList<>();

        for (UUID templateId : command.seatTemplateIds()) {
            Optional<SeatAssignments> seat = seatInventoryRepository
                    .findByFlightIdAndSeatTemplateIdAndFlightDate(command.flightId(), templateId,flightDate);

            if(seat.isEmpty()){
                throw new SeatNotFound(templateId);
            }
            seatsToBeBooked.add(seat.get());
        }

        seatInventoryService.bookedHoldSeatsOrThrow(seatsToBeBooked,command.bookingId());
        seatInventoryRepository.saveAll(seatsToBeBooked);

        //Remove expire event once seat is booked
        expiringSeatHoldManager.removeHold(flightId,command.customerId());

        return new SeatBookedResult(true);
    }
}
