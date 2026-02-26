package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.ReleasedHoldSeatCommand;

public interface ReleaseHoldSeatUseCase {
    void releaseHoldSeat(ReleasedHoldSeatCommand releasedHoldSeatCommand);
}
