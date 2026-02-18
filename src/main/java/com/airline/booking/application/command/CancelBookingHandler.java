package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingFailed;
import com.airline.flightmgmt.domain.SeatInventory;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.flightmgmt.exception.SeatLockingFailedException;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
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
        List<String> seatNumbers = booking.getSeats().stream().map(BookingSeat::getSeatNumber).toList();
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
        //TODO: Move this integration service or Fire event and handle by inventory service to release seats
        try {
            seatInventoryRepository.saveAll(seats);
        } catch (OptimisticLockingFailureException e) {
            throw new SeatLockingFailedException("Seat release failed due to concurrent modification; please retry");
        }

        bookingService.cancel(booking); // Invoke domain service to handle status change and invariants (including tickets)

        try {
            bookingRepository.save(booking);
        } catch (OptimisticLockingFailureException e) {
            throw new BookingFailed("Booking cancellation failed due to concurrent modification; please retry");
        }

        //TODO:Fire BookingCancelledEvent  , and start refund process (if payment is completed) asynchronously in event handler

        return new CancelBookingResult(bookingId, booking.getBookingReference(), "CANCELLED");
    }
}