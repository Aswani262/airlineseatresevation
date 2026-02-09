package com.airline.booking.api;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.BookSeatUseCase;
import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookSeatUseCase bookSeatUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;

    /**
     * 1) Book seats (single-leg) -> creates DRAFT booking + locks seats in DB (AVAILABLE -> LOCKED)
     */
    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public BookSeatResult book( @RequestBody BookSeatCommand command) {
        return bookSeatUseCase.book(command);
    }

    /**
     * 3) Cancel booking -> releases seats (LOCKED -> AVAILABLE for DRAFT, BOOKED -> AVAILABLE for CONFIRMED per policy)
     */
    @PostMapping("/{bookingId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public CancelBookingResult cancel(@PathVariable UUID bookingId,
                                       @RequestBody CancelBookingCommand command) {
        // bookingId is authoritative from URL
        command.setBookingId(bookingId);
        return cancelBookingUseCase.cancel(command);
    }
}
