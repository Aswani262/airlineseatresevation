package com.airline.flightmgmt.application.command;

import com.airline.booking.exception.SeatNotFound;
import com.airline.flightmgmt.api.dto.SeatReleaseResult;
import com.airline.flightmgmt.application.command.dto.ReleaseBookedSeatCommand;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class ReleaseBookedSeatHandler implements ReleaseBookedSeatUseCase {
    private final ISeatInventoryCommandRepository repository;
    private final ISeatInventoryService service;

    @Transactional
    @Override
    public void releaseBookedSeats(ReleaseBookedSeatCommand command) {

        List<SeatAssignments> seats = command.seatTemplateIds().stream()
                .map(id -> repository.findByFlightIdAndSeatTemplateId(command.flightId(), id)
                        .orElseThrow(SeatNotFound::new))
                .collect(Collectors.toList());

        service.releaseBookedSeats(seats);
        repository.saveAll(seats);

    }

}