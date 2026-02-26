package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;

import java.util.UUID;

public interface CancelBookingUseCase {
    CancelBookingResult cancel(UUID bookingId, CancelBookingCommand command);
}
