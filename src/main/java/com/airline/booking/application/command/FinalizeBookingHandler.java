package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.dto.FinalizeBookingCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.BookingCoreService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.events.BookingFinalizationFailedEvent;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class FinalizeBookingHandler implements FinalizeBookingUseCase {

    private final IBookingCommandRepository bookingRepository;
    private final BookingCoreService bookingCoreService;
    private final SeatInventoryIntegrationService seatInventoryService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public ConfirmBookingResult bookingFinalize(FinalizeBookingCommand command) {

        UUID bookingId = command.bookingId();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFound(bookingId));

        //idempotent
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            List<ConfirmBookingResult.TicketIssued> tickets = booking.getTickets().stream()
                    .map(t -> new ConfirmBookingResult.TicketIssued(t.getPassengerId(), t.getTicketNumber()))
                    .collect(Collectors.toList());
            return new ConfirmBookingResult(bookingId, booking.getBookingReference(), "CONFIRMED", tickets);
        }

        List<UUID> seatTemplateIds = booking.getSeats().stream()
                .map(BookingSeat::getSeatTemplateId)
                .toList();

        boolean seatsWereConfirmed = false;

        try {
             seatInventoryService.bookHoldSeat(booking.getFlightId(), seatTemplateIds,booking.getCustomerId(),bookingId);

             seatsWereConfirmed = true;

             bookingCoreService.finalizeBooking(booking);

             bookingRepository.save(booking);

            List<ConfirmBookingResult.TicketIssued> tickets = booking.getTickets().stream()
                    .map(t -> new ConfirmBookingResult.TicketIssued(t.getPassengerId(), t.getTicketNumber()))
                    .toList();

            return new ConfirmBookingResult(bookingId, booking.getBookingReference(), "CONFIRMED", tickets);

        } catch (Throwable ex) {
            if (seatsWereConfirmed) {
               // If payment failed or finalization is failed, release the seat
                eventPublisher.publish(new BookingFinalizationFailedEvent(booking.getFlightId(),seatTemplateIds,booking.getCustomerId(),bookingId));
            }
            throw ex;
        }
    }
}