package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;

public interface CancelBookingUseCase {
    CancelBookingResult cancelBooking(CancelBookingCommand paymentExpired);

}
