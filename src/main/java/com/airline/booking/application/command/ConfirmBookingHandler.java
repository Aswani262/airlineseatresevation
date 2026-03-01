package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmedBookingResult;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.exception.BookingConfirmationFailedExpection;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.flightmgmt.exception.SeatHoldingExpiredException;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.events.BookingConfirmationFailedEvent;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class ConfirmBookingHandler implements ConfirmBookingUseCase {

    private final IBookingService bookingCoreService;
    private final IBookingCommandRepository bookingRepository;
    private final SeatInventoryIntegrationService seatInventoryService; // still used for extend only
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public ConfirmedBookingResult confirmBooking(ConfirmBookingCommand command) {

        Booking booking = bookingCoreService.createPending(command);

        List<UUID> seatTemplateIds = booking.getSeats().stream()
                .map(BookingSeat::getSeatTemplateId)
                .toList();

        try {
            //Extend the hold expiry time of payment - if any exception occur then release the hold seat
            // and attach the booking id with id , which we use to release only seat which are associated
            // with this booking in case of cancellation
             seatInventoryService.extendSeatExpiryTimeForPayment(
                    command.getFlightId(),
                    seatTemplateIds, command.getCustomerId(), booking.getId()
            );

            bookingRepository.save(booking);

            return new ConfirmedBookingResult(
                    booking.getId(),
                    booking.getBookingReference(),
                    booking.getStatus().toString()
            );

        } catch (Throwable ex) {
                //Release seat which hold by this customer , this event will be listen by interested
                //Service - like Inventory Service or Notification service - if using Pub/Sub

               //If using queue - point to point , you have to fire two event like , SendNotificationEvent (notification queue)
               // ReleaseSeatEvent (Inventory Queue)

                //Make sure publish of event should be part of the same transaction - Need to check
                eventPublisher.publish(new BookingConfirmationFailedEvent(
                        command.getFlightId(),
                        seatTemplateIds,
                        command.getCustomerId()
                ));

            throw new BookingConfirmationFailedExpection(ex);
        }
    }
}