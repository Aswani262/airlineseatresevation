package com.airline.booking.application.command;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.service.SeatInventoryService;
import com.airline.booking.repository.BookingRepository;
import com.airline.booking.service.BookingCoreService;
import com.airline.booking.domain.model.Booking;
import com.airline.shared.annoation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

//Application service orchestrating the booking process, coordinating between domain services and repositories.
// It handles the main flow of booking a seat, including creating a draft, locking seats, and persisting the booking.
@ApplicationService
@RequiredArgsConstructor
public class BookSeatHandler implements BookSeatUseCase {

    //Put in properties file or DB in real app, but hardcoded here for simplicity
    private static final int HOLD_MINUTES = 10;
    private static final Duration HOLD_TTL = Duration.ofMinutes(HOLD_MINUTES);

    private final BookingCoreService bookingCoreService;
    private final SeatInventoryService seatInventoryService;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public BookSeatResult book(BookSeatCommand command) {

        // 1) Draft booking data (validation, totals, seat normalization done here or in seat service)
        Booking booking = bookingCoreService.createDraft(command);

        // 2) Lock seats (core/domain service)
        var lockResult = seatInventoryService.lockSeats(
                command.getFlightId(),
                booking.getId(),
                booking.getSeats().stream().map(BookingSeat::getSeatNumber).toList(),
                HOLD_TTL
        );
        seatInventoryService.ensureLockedOrThrow(lockResult);

        // Set the hold expiration from the lock result
        booking.setHoldExpiresAt(lockResult.expiresAt());

        // 3) Persist booking aggregate
        bookingRepository.saveBooking(booking);

        return new BookSeatResult(
                booking.getId(),
                booking.getBookingReference(),
                booking.getStatus().toString(),
                lockResult.expiresAt()
        );
    }
}