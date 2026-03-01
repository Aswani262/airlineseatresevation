package com.airline.flightmgmt.application.command;

import com.airline.flightmgmt.api.dto.SeatBookedResult;
import com.airline.flightmgmt.application.command.dto.ConfirmSeatCommand;

public interface BookSeatUseCase {
    SeatBookedResult bookHoldSeat(ConfirmSeatCommand confirmSeatCommand);
}
