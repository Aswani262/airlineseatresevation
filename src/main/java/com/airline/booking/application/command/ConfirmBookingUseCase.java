package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;

public interface ConfirmBookingUseCase {
    ConfirmBookingResult bookingFinalize(ConfirmBookingCommand command);
}
