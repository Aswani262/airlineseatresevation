package com.airline.booking.application.command;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.dto.BookSeatCommand;

public interface BookSeatUseCase {
    BookSeatResult book(BookSeatCommand command);
}
