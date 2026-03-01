package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmedBookingResult;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
//Every use case should have a single method that takes a command and returns a result.
// This is the standard pattern for use cases in a clean architecture.
// and here also only implemented by separate handler class,
// so that we can have multiple implementations if needed (e.g. for different booking flows or for testing).
// and to stop sharing code between different use cases, which can lead to tight coupling and maintenance issues.
// Single Responsibility Principle: Each use case should have a
// single responsibility and should not be responsible for multiple actions or operations.
// This promotes separation of concerns and makes the code easier to maintain and test.
public interface ConfirmBookingUseCase {
    ConfirmedBookingResult confirmBooking(ConfirmBookingCommand command);
}
