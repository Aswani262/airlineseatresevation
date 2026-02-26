package com.airline.booking.service.core;

import com.airline.booking.application.command.dto.InitiateBookingSeatCommand;
import com.airline.booking.domain.model.Booking;

public interface IBookingService {

    Booking createPending(InitiateBookingSeatCommand cmd);

    // New method added for cancel logic (validation and state change)
    void cancel(Booking booking);

    void confirm(Booking booking);
}
