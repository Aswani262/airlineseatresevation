package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.ReleaseBookedSeatCommand;

public interface ReleaseBookedSeatUseCase {
    void releaseBookedSeats(ReleaseBookedSeatCommand command);

}
