package com.airline.flightmgmt.application.command;

import com.airline.shared.model.SeatLockResult;

public interface LockSeatUseCase {
    SeatLockResult lockSeat(LockSeatCommand command);
}
