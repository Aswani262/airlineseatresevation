package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.application.command.dto.ExtendExpiryTimeCommand;
import com.airline.shared.model.SeatLockResult;

public interface ExtendExpiryTimeUseCase {
    SeatLockResult extendExpiryTime(ExtendExpiryTimeCommand extendExpiryTimeCommand);
}
