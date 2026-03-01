package com.airline.booking.service.core;

import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.domain.model.Booking;

public interface IBookingService {

    Booking createPending(ConfirmBookingCommand cmd);

    // New method added for cancel logic (validation and state change)
    void cancel(Booking booking);

    void finalizeBooking(Booking booking);
}
