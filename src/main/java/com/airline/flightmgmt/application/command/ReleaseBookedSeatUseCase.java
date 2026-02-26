package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.api.dto.SeatReleaseResult;
import com.airline.flightmgmt.application.command.dto.ReleaseBookedSeatCommand;
import org.springframework.transaction.annotation.Transactional;

public interface ReleaseBookedSeatUseCase {
    void releaseBookedSeats(ReleaseBookedSeatCommand command);

}
