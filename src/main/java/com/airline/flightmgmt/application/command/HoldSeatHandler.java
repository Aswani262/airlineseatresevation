package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.CheckSeatHoldStatusCommand;
import com.airline.flightmgmt.application.command.dto.HoldSeatCommand;
import com.airline.flightmgmt.domain.HoldStage;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.exception.SeatAlreadyHeldException;
import com.airline.flightmgmt.exception.SeatHoldingExpiredException;
import com.airline.flightmgmt.exception.SeatHoldingFailedException;
import com.airline.flightmgmt.exception.SeatNotAvailableException;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.model.SeatLockResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// This class is responsible for handling the command to reserve a seat on a flight.
// It will interact with the domain layer to perform the necessary operations to reserve a seat,
// such as checking availability, locking the seat, and confirming the reservation.


//We should use one term - lock or hold , using two different term for same feature , create a
//Communication gap between developer and BA

@ApplicationService
@RequiredArgsConstructor
public class HoldSeatHandler implements HoldSeatUseCase {

    private final ISeatInventoryCommandRepository seatInventoryRepository;
    private final ISeatInventoryService seatInventoryService;

    //Hold minute extend according to stages of booking process, for example,
    // we will extend hold time for payment stage than seat selection stage.
    private static final int HOLD_MINUTES = 10;

    @Override
    @Transactional
    public SeatLockResult holdSeat(HoldSeatCommand command) {

        //TODO: Validate command

        UUID flightId = command.flightId();
        List<UUID> requestedSeatTemplateIds = command.seatTemplateId();

        List<SeatAssignments> seatsToHold = new ArrayList<>();

        //If any of the requested seats do not exist in Seat Assignments , means that is AVAILABLE
        for (UUID templateId : requestedSeatTemplateIds) {
            SeatAssignments seat = seatInventoryRepository
                    .findByFlightIdAndSeatTemplateId(flightId, templateId)
                    .orElseGet(() -> {
                        SeatAssignments newSeat = new SeatAssignments();
                        newSeat.setFlightId(flightId);
                        newSeat.setHoldStage(HoldStage.SEAT_SELECTION);
                        newSeat.setSeatTemplateId(templateId);
                        newSeat.setStatus(SeatStatus.AVAILABLE);
                        return newSeat;
                    });

            seatsToHold.add(seat);
        }

        seatInventoryService.validateAndPrepareSeatsForHolding(seatsToHold,HoldStage.SEAT_SELECTION);

        SeatLockResult result = seatInventoryService.holdSeats(seatsToHold, Duration.ofMinutes(HOLD_MINUTES));

        try {
            seatInventoryRepository.saveAll(seatsToHold);
        } catch (OptimisticLockingFailureException ex){
            // This exception can occur if another transaction has modified the same seat records after we read them and before we saved them.
            // In this case, we can treat it as a failure to hold the seats and return an appropriate response to the user.
            // This exception qualified as retry able because that can be happend with any changes
            // Give user an option to retry again if this exception occur
            throw new SeatHoldingFailedException("Failed to hold seats due to concurrent modification. Please try again.");
        }
        return result;
    }

}
