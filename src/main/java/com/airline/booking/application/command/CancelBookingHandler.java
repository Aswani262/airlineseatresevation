package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.airline.booking.application.command.dto.CancelationReason;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingStage;
import com.airline.booking.domain.model.BookingStatus;
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
public class CancelBookingHandler implements CancelBookingUseCase {

    private final IBookingCommandRepository bookingRepository;
    private final IBookingService bookingService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public CancelBookingResult cancelBooking(CancelBookingCommand command) {

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new BookingNotFound(command.bookingId()));

        BookingStatus currentStatus = booking.getStatus();

        BookingStage bookingStage = booking.getBookingStage();

        // Idempotency - already cancelled or failed
        if (currentStatus == BookingStatus.CANCELLED || currentStatus == BookingStatus.FAILED) {
            return new CancelBookingResult(booking.getId(), currentStatus.name());
        }

        //If booking in confirmed status , and getting seat time out cancellation reason , just ignore it
        //because there is a possibilty that we are getting false event or event reach us late
        //No need to fire a event to release the seat

        //To make sure the order event all related event should use one topic
        if(command.reason() == CancelationReason.SEAT_HOLDING_TIME_OUT && currentStatus == BookingStatus.CONFIRMED){
            return new CancelBookingResult(booking.getId(), currentStatus.name());
        }

        bookingService.cancel(booking,CancelationReason.SEAT_HOLDING_TIME_OUT);

        bookingRepository.save(booking);

        //Only fire the event to release the in case of cancel by user
        //Because cancellation due to seat time out , already released the seat
        if(currentStatus == BookingStatus.CONFIRMED && command.reason() == CancelationReason.BY_USER){
            //This event will release the seat + initiate the refund
            eventPublisher.publish(new BookingCancelledEvent(booking.getId(),booking.getFlightId()));
        }

        return new CancelBookingResult(
                command.bookingId(),
                "CANCELLED"
        );
    }
}