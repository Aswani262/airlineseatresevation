package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelConfirmedBookingResult;

public interface CancelConfirmedBookingUseCase {
    CancelConfirmedBookingResult cancelConfirmedBooking(CancelBookingCommand command);
}
