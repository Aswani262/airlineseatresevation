package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelConfirmedBookingResult;
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
public class CancelConfirmedBookingHandler implements CancelConfirmedBookingUseCase {

    private final IBookingCommandRepository bookingRepository;
    private final IBookingService bookingService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public CancelConfirmedBookingResult cancelConfirmedBooking(CancelBookingCommand command) {

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new BookingNotFound(command.bookingId()));

        BookingStatus currentStatus = booking.getStatus();

        if (currentStatus == BookingStatus.CANCELLED) {
            return new CancelConfirmedBookingResult(booking.getId(), booking.getBookingReference(), "CANCELLED");
        }

        if (currentStatus == BookingStatus.EXPIRED) {
            return new CancelConfirmedBookingResult(booking.getId(), booking.getBookingReference(), "EXPIRED");
        }

        try {

            bookingService.cancel(booking);

            bookingRepository.save(booking);

            //Release seat which are only associated with this booking Id
            //This event will be listen by interested consumer like notification service , inventory service
            eventPublisher.publish( new BookingCancelledEvent(booking.getFlightId(),command.bookingId()));

        } catch (Throwable ex){
            throw new BookingCancelationFailedExcpetion(ex);
        }
        return new CancelConfirmedBookingResult(command.bookingId(), booking.getBookingReference(), "CANCELLED");
    }
}