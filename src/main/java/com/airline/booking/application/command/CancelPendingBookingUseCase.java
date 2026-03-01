package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelPendingBookingCommand;
import com.airline.booking.application.command.dto.CancelPendingBookingResult;

public interface CancelPendingBookingUseCase {
    CancelPendingBookingResult cancelPendingBooking(CancelPendingBookingCommand paymentExpired);

}
