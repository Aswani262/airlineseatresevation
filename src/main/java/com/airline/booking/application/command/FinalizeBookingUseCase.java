package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.dto.FinalizeBookingCommand;

public interface FinalizeBookingUseCase {
    ConfirmBookingResult bookingFinalize(FinalizeBookingCommand command);
}
