package com.airline.booking.service;

import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.Booking;

public interface IBookingCoreService {

    Booking createDraft(BookSeatCommand cmd);
}
