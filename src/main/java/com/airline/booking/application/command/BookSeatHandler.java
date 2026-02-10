package com.airline.booking.application.command;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.SeatInventory;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.repository.ISeatInventoryCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.booking.service.core.ISeatInventoryService;
import com.airline.shared.annoation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

//Application service orchestrating the booking process, coordinating between domain services and repositories.
// It handles the main flow of booking a seat, including creating a draft, locking seats, and persisting the booking.
@ApplicationService
@RequiredArgsConstructor
public class BookSeatHandler implements BookSeatUseCase {

    //Put in properties file or DB in real app, but hardcoded here for simplicity
    private static final int HOLD_MINUTES = 10;
    private static final Duration HOLD_TTL = Duration.ofMinutes(HOLD_MINUTES);

    private final IBookingService bookingCoreService;
    private final ISeatInventoryService seatInventoryService;
    private final IBookingCommandRepository bookingRepository;
    private final ISeatInventoryCommandRepository seatInventoryRepository;

    @Override
    @Transactional
    public BookSeatResult book(BookSeatCommand command) {

        // 1) Draft booking data (validation, totals, seat normalization done here or in seat service)
        Booking booking = bookingCoreService.createDraft(command);

        // 2) Prepare seats
        List<String> seatNumbers = booking.getSeats().stream().map(BookingSeat::getSeatNumber).toList();
        List<String> normalizeSeats = seatInventoryService.normalizeSeats(seatNumbers);


        List<SeatInventory> seats = seatInventoryRepository.findByFlightIdAndSeatNumberIn(command.getFlightId(), normalizeSeats);

        if (seats.size() != normalizeSeats.size()) {
            throw new IllegalStateException("One or more seats not found");
        }

        // Lock seats - this will set the lock expiration time on the seat inventory records
        var lockResult = seatInventoryService.lockSeats(seats, booking.getId(), HOLD_TTL);
        if (!lockResult.success()) {
            throw new IllegalStateException("One or more seats are not available");
        }

        // Persist updated seats
        try {
            seatInventoryRepository.saveAll(seats);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Seat locking failed due to concurrent modification; please retry", e);
        }

        // Set the hold expiration from the lock result
        booking.setHoldExpiresAt(lockResult.expiresAt());

        try {
            // 3) Persist booking aggregate
            bookingRepository.save(booking);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Booking creation failed due to concurrent modification; please retry", e);
        }

        return new BookSeatResult(
                booking.getId(),
                booking.getBookingReference(),
                booking.getStatus().toString(),
                lockResult.expiresAt()
        );
    }
}