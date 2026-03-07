package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.HoldSeatCommand;
import com.airline.flightmgmt.domain.HoldStage;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.repository.IFlightCacheRepository;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.core.ExpiringSeatHoldManager;
import com.airline.flightmgmt.service.core.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.exception.ErrorNotification;
import com.airline.shared.exception.StructuralException;
import com.airline.shared.model.SeatLockResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

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
    private final ExpiringSeatHoldManager expiringSeatHoldManager;
    private final IFlightCacheRepository flightCacheRepository;

    //Hold minute extend according to stages of booking process, for example,
    // we will extend hold time for payment stage than seat selection stage.
    private static final int HOLD_MINUTES = 10;

    @Override
    @Transactional
    @Retryable(
            retryFor = {OptimisticLockingFailureException.class, TimeoutException.class, SocketTimeoutException.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 800)
    )
    public SeatLockResult holdSeat(HoldSeatCommand command) {

        ErrorNotification errors = seatInventoryService.validate(command);
        if(errors.hasErrors()){
            throw new StructuralException(errors);
        }

        UUID flightId = command.flightId();

        //Get Flight Date from cache , to use in query
        //Because of partition purning of flightDate
        //Flight information is not continuosly updating we can do the caching

        //Try to use flight date in approx every query to use partition as optimized way
        // rather than scaning all the partition
        LocalDate flightDate = flightCacheRepository.getFlight(flightId).getFlightDate();

        List<UUID> requestedSeatTemplateIds = command.seatTemplateId();
        UUID customerId = command.customerId();

        List<SeatAssignments> seatsToHold = new ArrayList<>();

        //If any of the requested seats do not exist in seat assignments , means that is available
        for (UUID templateId : requestedSeatTemplateIds) {
            SeatAssignments seat = seatInventoryRepository
                    .findByFlightIdAndSeatTemplateIdAndFlightDate(flightId, templateId,flightDate)
                    .orElseGet(() -> {
                        SeatAssignments newSeat = new SeatAssignments();
                        newSeat.setId(UUID.randomUUID());
                        newSeat.setFlightId(flightId);
                        newSeat.setHoldStage(HoldStage.SEAT_SELECTION);
                        newSeat.setSeatTemplateId(templateId);
                        newSeat.setStatus(SeatStatus.AVAILABLE);
                        newSeat.setCustomerId(customerId);
                        newSeat.setFlightDate(flightDate);
                        return newSeat;
                    });
            seatsToHold.add(seat);
        }

        seatInventoryService.validateAndPrepareSeatsForHolding(seatsToHold,HoldStage.SEAT_SELECTION,customerId);

        SeatLockResult result = seatInventoryService.holdSeats(seatsToHold, Duration.ofMinutes(HOLD_MINUTES));

        seatInventoryRepository.saveAll(seatsToHold);

        //Schedule in memory expiring , which fire event once seat holding expired , delete from DB means release the seat
        expiringSeatHoldManager.holdSeat(flightId,customerId,HoldStage.SEAT_SELECTION, seatsToHold.get(0).getLockExpiresAt());

        return result;
    }

}
