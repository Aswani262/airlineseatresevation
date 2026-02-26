package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.ReleasedHoldSeatCommand;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class ReleaseHoldSeatHandler implements ReleaseHoldSeatUseCase {

    private final ISeatInventoryCommandRepository seatInventoryRepository;

    private final ISeatInventoryService seatInventoryService;

    @Override
    public void releaseHoldSeat(ReleasedHoldSeatCommand command) {

        UUID flightId = command.flightId();
        List<UUID> seatTemplateIds = command.seatTemplateIds();

        List<SeatAssignments> seats = seatTemplateIds.stream()
                .map(templateId -> seatInventoryRepository
                        .findByFlightIdAndSeatTemplateId(flightId, templateId)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (seats.isEmpty()) {
            return; // seats already released by scheduler or never existed
        }
        seatInventoryService.releaseLockedSeats(seats);

        seatInventoryRepository.saveAll(seats);
    }
}
