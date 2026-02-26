package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.ExtendExpiryTimeCommand;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.exception.SeatHoldingExpiredException;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.model.SeatLockResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class ExtendExpiryTimeHandler implements ExtendExpiryTimeUseCase {

    private final int PAYMENT_EXTENSION_MINUTES = 15;

    private final ISeatInventoryService seatInventoryService;
    private final ISeatInventoryCommandRepository seatInventoryRepository;

    @Override
    @Transactional
    public SeatLockResult extendExpiryTime(ExtendExpiryTimeCommand command) {
        // TODO: input validation (not null, etc.)

        UUID flightId = command.flightId();
        UUID customerId = command.customerId();

        List<SeatAssignments> seats = command.seatTemplateIds().stream()
                .map(templateId -> seatInventoryRepository
                        .findByFlightIdAndSeatTemplateId(flightId, templateId)
                        .orElseThrow(() -> new SeatHoldingExpiredException(
                            "Seat hold expired or not found. Please select seats again.")))
                .collect(Collectors.toList());

        SeatLockResult result = seatInventoryService.extendSeatExpiryForPayment(
                seats, Duration.ofMinutes(PAYMENT_EXTENSION_MINUTES),customerId);

        seatInventoryRepository.saveAll(seats);

        return result;
    }

}