package com.airline.flightmgmt.application.command;

import com.airline.booking.exception.SeatNotFound;
import com.airline.flightmgmt.api.dto.SeatBookedResult;
import com.airline.flightmgmt.application.command.dto.ConfirmSeatCommand;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class ConfirmSeatHandler implements ConfirmSeatUseCase {

    private final ISeatInventoryCommandRepository seatInventoryRepository;
    private final ISeatInventoryService seatInventoryService;

    @Override
    public SeatBookedResult confirmSeat(ConfirmSeatCommand command) {

        List<SeatAssignments> seatsToBeConfirmed = new ArrayList<>();

        for (UUID templateId : command.seatTemplateIds()) {
            Optional<SeatAssignments> seat = seatInventoryRepository
                    .findByFlightIdAndSeatTemplateId(command.flightId(), templateId);

            if(seat.isEmpty()){
                throw new SeatNotFound();
            }

            seatsToBeConfirmed.add(seat.get());
        }

        seatInventoryService.confirmLockedSeatsOrThrow(seatsToBeConfirmed);
        seatInventoryRepository.saveAll(seatsToBeConfirmed);

        return new SeatBookedResult(true);
    }
}
