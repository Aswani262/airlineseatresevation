package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingCancelledFailedExcpetion;
import com.airline.booking.exception.BookingFailed;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.flightmgmt.application.command.dto.ReleaseBookedSeatCommand;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.flightmgmt.exception.SeatHoldingFailedException;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.events.ReleaseBookedSeatEvent;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class CancelBookingHandler implements CancelBookingUseCase {

    private final IBookingCommandRepository bookingRepository;
    private final IBookingService bookingService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public CancelBookingResult cancel(UUID bookingId,CancelBookingCommand command) {

        //TODO:Validate Command

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFound(bookingId));

        BookingStatus currentStatus = booking.getStatus();

        if (currentStatus == BookingStatus.CANCELLED) {
            return new CancelBookingResult(booking.getId(), booking.getBookingReference(), "CANCELLED");
        }

        if (currentStatus == BookingStatus.EXPIRED) {
            return new CancelBookingResult(booking.getId(), booking.getBookingReference(), "EXPIRED");
        }

        List<UUID> seatTemplateIds = booking.getSeats().stream().map(BookingSeat::getSeatTemplateId).toList();

        try {
            bookingService.cancel(booking);

            bookingRepository.save(booking);

            eventPublisher.publish( new ReleaseBookedSeatEvent(booking.getFlightId(),seatTemplateIds));

        } catch (Throwable ex){
            throw new BookingCancelledFailedExcpetion();
        }
        return new CancelBookingResult(bookingId, booking.getBookingReference(), "CANCELLED");
    }
}