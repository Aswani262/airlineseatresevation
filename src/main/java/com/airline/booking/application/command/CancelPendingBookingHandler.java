package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelPendingBookingCommand;
import com.airline.booking.application.command.dto.CancelPendingBookingResult;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingCancelationFailedExcpetion;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.events.BookingCancelledEvent;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class CancelPendingBookingHandler implements CancelPendingBookingUseCase {

    private final IBookingCommandRepository bookingRepository;
    private final IBookingService bookingService;

    @Override
    @Transactional
    public CancelPendingBookingResult cancelPendingBooking(CancelPendingBookingCommand command) {

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new BookingNotFound(command.bookingId()));

        BookingStatus currentStatus = booking.getStatus();

        // Idempotency - already cancelled
        if (currentStatus == BookingStatus.CANCELLED) {
            return new CancelPendingBookingResult(booking.getId(), "CANCELLED");
        }

        if (currentStatus != BookingStatus.PENDING) {
            throw new BookingCancelationFailedExcpetion(
                    "Cannot cancel pending booking. Current status: " + currentStatus);
        }

        bookingService.cancel(booking);

        bookingRepository.save(booking);

        return new CancelPendingBookingResult(
                command.bookingId(),
                "CANCELLED"
        );
    }
}