package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.ExtendExpiryTimeCommand;
import com.airline.flightmgmt.domain.HoldStage;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.exception.SeatHoldingExpiredException;
import com.airline.flightmgmt.repository.IFlightCacheRepository;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.core.ExpiringSeatHoldManager;
import com.airline.flightmgmt.service.core.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.exception.ErrorNotification;
import com.airline.shared.exception.StructuralException;
import com.airline.shared.model.SeatLockResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class ExtendExpiryTimeHandler implements ExtendExpiryTimeUseCase {

    private final int PAYMENT_EXTENSION_MINUTES = 15;

    private final ISeatInventoryService seatInventoryService;
    private final ISeatInventoryCommandRepository seatInventoryRepository;
    private final ExpiringSeatHoldManager expiringSeatHoldManager;
    private final IFlightCacheRepository flightCacheRepository;

    @Override
    @Transactional
    public SeatLockResult extendExpiryTime(ExtendExpiryTimeCommand command) {

        ErrorNotification errors = seatInventoryService.validate(command);

        if(errors.hasErrors()){
            throw new StructuralException(errors);
        }

        UUID flightId = command.flightId();

        //Get Flight date (partition key ) from cache to used in query
        LocalDate flightDate = flightCacheRepository.getFlight(flightId).getFlightDate();

       List<SeatAssignments> seats = command.seatTemplateIds().stream()
                .map(templateId -> seatInventoryRepository
                        .findByFlightIdAndSeatTemplateIdAndFlightDate(flightId, templateId,flightDate)
                        .orElseThrow(() -> new SeatHoldingExpiredException(
                            "Seat hold expired or not found. Please select seats again.")))
                .collect(Collectors.toList());

        SeatLockResult result = seatInventoryService.extendSeatExpiryForPayment(
                seats, Duration.ofMinutes(PAYMENT_EXTENSION_MINUTES),command.customerId(),command.bookingId());

        seatInventoryRepository.saveAll(seats);

        //Schedule expiry of seat in manager
        expiringSeatHoldManager.holdSeat(flightId,command.customerId(), HoldStage.PAYMENT,seats.get(0).getLockExpiresAt());

        return result;
    }

}