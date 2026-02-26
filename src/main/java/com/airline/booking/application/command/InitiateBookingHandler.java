package com.airline.booking.application.command;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.dto.InitiateBookingSeatCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.exception.BookingConfirmationFailedExpection;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.events.ReleaseHoldSeatEvent;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class InitiateBookingHandler implements BookSeatUseCase {

    private final IBookingService bookingCoreService;
    private final IBookingCommandRepository bookingRepository;
    private final SeatInventoryIntegrationService seatInventoryService; // still used for extend only
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public BookSeatResult initiateBooking(InitiateBookingSeatCommand command) {

        Booking booking = bookingCoreService.createPending(command);

        List<UUID> seatTemplateIds = booking.getSeats().stream()
                .map(BookingSeat::getSeatTemplateId)
                .toList();

        boolean seatsWereExtended = false;

        try {

            //Extend the hold expiry time
             seatInventoryService.extendSeatExpiryTimeForPayment(
                    command.getFlightId(),
                    seatTemplateIds
            );

            seatsWereExtended = true;

            bookingRepository.save(booking);

            return new BookSeatResult(
                    booking.getId(),
                    booking.getBookingReference(),
                    booking.getStatus().toString()
            );

        } catch (Throwable ex) {
            if (seatsWereExtended) {
                eventPublisher.publish(new ReleaseHoldSeatEvent(
                        command.getFlightId(),
                        seatTemplateIds
                ));
            }
            throw new BookingConfirmationFailedExpection("Booking confirmation failed");
        }
    }
}