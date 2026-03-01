package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.HoldSeatCommand;
import com.airline.shared.model.SeatLockResult;

public interface HoldSeatUseCase {
    SeatLockResult holdSeat(HoldSeatCommand command);
}
