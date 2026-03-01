package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.ReleasedHoldSeatCommand;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.repository.IFlightCacheRepository;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.core.ExpiringSeatHoldManager;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class ReleaseHoldSeatHandler implements ReleaseHoldSeatUseCase {

    private final ISeatInventoryCommandRepository seatInventoryRepository;
    private final ExpiringSeatHoldManager expiringSeatHoldManager;
    private final IFlightCacheRepository flightCacheRepository;


    @Transactional
    @Override
    public void releaseHoldSeat(ReleasedHoldSeatCommand command) {

        UUID flightId = command.flightId();

       LocalDate flightDate = flightCacheRepository.getFlight(flightId).getFlightDate();

        List<UUID> seatTemplateIds = command.seatTemplateIds();

        List<SeatAssignments> seats = seatTemplateIds.stream()
                .map(templateId -> seatInventoryRepository
                        .findByFlightIdAndSeatTemplateIdAndFlightDate(flightId, templateId,flightDate)
                        //Make it linent - may be already release by schedular or Expriy hold manager
                        .orElse(null))
                .filter(Objects::nonNull)
                //Only release seat which are in hold status and hold is acquired by this customer
                .filter(seatAssignments -> seatAssignments.getStatus() == SeatStatus.HOLD && seatAssignments.getCustomerId().equals(command.customerId()))
                .collect(Collectors.toList());

        if (seats.isEmpty()) {
            return; //Seat already released by schedular/expring manager
        }

        //Delete from seat assigment , as seat assigment only contains
        // the Hold and booked status seat
        seatInventoryRepository.deleteAll(seats);

        seats.forEach(seat -> expiringSeatHoldManager.removeHold(flightId,seat.getSeatTemplateId()));

    }
}
