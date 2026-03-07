package com.airline.booking.api;

import com.airline.booking.api.dto.ConfirmedBookingResult;
import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.booking.application.command.ConfirmBookingUseCase;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.airline.booking.application.command.dto.CancelConfirmedBookingResult;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final ConfirmBookingUseCase bookSeatUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;


    //We should use different DTO/POJO on different layers,
    //but for simplicity using same DTO for both API and application layer


    //Don't put any validation or business logic in controller,
    // it should be in application service or domain model.
    // Controller should just orchestrate the request and response.
    // Another reason to keep controller thin is that we may have multiple entry points
    // (e.g. REST API, gRPC, Event Handler) and we want to reuse the same application service logic across them as
    // much as possible without duplicating code or
    // logic in each entry point (make a single entry point to the application layer).

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfirmedBookingResult confirmBooking(@RequestBody ConfirmBookingCommand command) {
        return bookSeatUseCase.confirmBooking(command);
    }

    @PutMapping("/{bookingId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public CancelBookingResult cancelBooking(@PathVariable UUID bookingId,
                                             @RequestBody CancelBookingCommand command) {

        return cancelBookingUseCase.cancelBooking(new CancelBookingCommand(bookingId,command.reason()));
    }
}
