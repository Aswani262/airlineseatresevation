package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.BookingCoreService;
import com.airline.flightmgmt.application.command.dto.ReleasedHoldSeatCommand;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.events.ReleaseHoldSeatEvent;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class ConfirmBookingHandler implements ConfirmBookingUseCase {

    private final IBookingCommandRepository bookingRepository;
    private final BookingCoreService bookingCoreService;
    private final SeatInventoryIntegrationService seatInventoryService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public ConfirmBookingResult bookingFinalize(ConfirmBookingCommand command) {

        UUID bookingId = command.getBookingId();
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

             seatInventoryService.confirmSeat(booking.getFlightId(), seatTemplateIds);
             seatsWereConfirmed = true;

             bookingCoreService.confirm(booking);

             bookingRepository.save(booking);

            List<ConfirmBookingResult.TicketIssued> tickets = booking.getTickets().stream()
                    .map(t -> new ConfirmBookingResult.TicketIssued(t.getPassengerId(), t.getTicketNumber()))
                    .toList();

            return new ConfirmBookingResult(bookingId, booking.getBookingReference(), "CONFIRMED", tickets);

        } catch (Exception ex) {
            if (seatsWereConfirmed) {
               // If payment failed , release the seat
                eventPublisher.publish(new ReleaseHoldSeatEvent(bookingId,seatTemplateIds));
            }
            throw ex;
        }
    }
}