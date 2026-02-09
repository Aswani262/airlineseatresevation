package com.airline.booking.service;

import com.airline.booking.application.command.dto.BookSeatCommand;

public interface IBookingCoreService {
    BookingCoreService.BookingDraft createDraft(BookSeatCommand cmd, int holdMinutes);
}
