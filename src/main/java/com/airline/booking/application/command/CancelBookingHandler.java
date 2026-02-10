package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.domain.model.SeatInventory;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.repository.ISeatInventoryCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.booking.service.core.ISeatInventoryService;
import com.airline.shared.annoation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class CancelBookingHandler implements CancelBookingUseCase {

    private final IBookingCommandRepository bookingRepository;
    private final ISeatInventoryService seatInventoryService;
    private final ISeatInventoryCommandRepository seatInventoryRepository;
    private final IBookingService bookingService;

    @Override
    @Transactional
    public CancelBookingResult cancel(CancelBookingCommand command) {

        UUID bookingId = command.getBookingId();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found: " + bookingId));

        BookingStatus currentStatus = booking.getStatus();

        if (currentStatus == BookingStatus.CANCELLED) {
            return new CancelBookingResult(booking.getId(), booking.getBookingReference(), "CANCELLED");
        }

        if (currentStatus == BookingStatus.EXPIRED) {
            return new CancelBookingResult(booking.getId(), booking.getBookingReference(), "EXPIRED");
        }

        // Prepare seats
        List<String> seatNumbers = booking.getSeats().stream().map(s -> s.getSeatNumber()).toList();
        List<String> normalized = seatInventoryService.normalizeSeats(seatNumbers);
        List<SeatInventory> seats = seatInventoryRepository.findByFlightIdAndSeatNumberIn(booking.getFlightId(), normalized);

        if (seats.size() != normalized.size()) {
            throw new IllegalStateException("One or more seats not found");
        }

        if (currentStatus == BookingStatus.DRAFT) {
            // release only seats locked by THIS booking
            seatInventoryService.releaseLockedSeatsOrThrow(seats, bookingId);
        } else if (currentStatus == BookingStatus.CONFIRMED) {
            // return seats to inventory (policy decision)
            seatInventoryService.releaseBookedSeats(seats);
        } else {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + currentStatus);
        }

        // Persist updated seats if changes were made
        try {
            seatInventoryRepository.saveAll(seats);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Seat release failed due to concurrent modification; please retry", e);
        }

        bookingService.cancel(booking); // Invoke domain service to handle status change and invariants (including tickets)

        try {
            bookingRepository.save(booking); // Persists changes to booking and children (tickets); optimistic locking via @Version
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Booking status changed concurrently; please retry", e);
        }

        return new CancelBookingResult(bookingId, booking.getBookingReference(), "CANCELLED");
    }
}