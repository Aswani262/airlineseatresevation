package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingHoldExpiredException;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.exception.SeatNotFound;
import com.airline.flightmgmt.domain.SeatInventory;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.booking.service.core.BookingCoreService;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class ConfirmBookingHandler implements ConfirmBookingUseCase {

    private final IBookingCommandRepository bookingRepository;
    private final ISeatInventoryService seatInventoryService;
    private final ISeatInventoryCommandRepository seatInventoryRepository;
    private final BookingCoreService bookingCoreService;

    @Override
    @Transactional
    public ConfirmBookingResult bookingFinalize(ConfirmBookingCommand command) {

        UUID bookingId = command.getBookingId();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFound( bookingId.toString()));

        BookingStatus originalStatus = booking.getStatus();

        bookingCoreService.confirm(booking);

        if (originalStatus == booking.getStatus()) {
            // Already CONFIRMED idempotent  return with existing tickets
            List<ConfirmBookingResult.TicketIssued> responseTickets = booking.getTickets().stream()
                    .map(t -> new ConfirmBookingResult.TicketIssued(t.getPassengerId(), t.getTicketNumber()))
                    .collect(Collectors.toList());
            return new ConfirmBookingResult(booking.getId(), booking.getBookingReference(), "CONFIRMED", responseTickets);
        }

        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        List<String> seatNumbers = booking.getSeats().stream().map(s -> s.getSeatNumber()).toList();
        List<String> normalized = seatInventoryService.normalizeSeats(seatNumbers);
        List<SeatInventory> seats = seatInventoryRepository.findByFlightIdAndSeatNumberIn(booking.getFlightId(), normalized);

        if (seats.size() != normalized.size()) {
            throw new SeatNotFound();
        }

        seatInventoryService.confirmLockedSeatsOrThrow(seats, bookingId);


        //TODO: Move to integration service and handle retries with backoff in case of optimistic locking failure (concurrent seat modifications)
        try {
            seatInventoryRepository.saveAll(seats);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Seat confirmation failed due to concurrent modification; please retry", e);
        }

        try {
            bookingRepository.save(booking);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Booking status changed concurrently; please retry", e);
        }

        List<ConfirmBookingResult.TicketIssued> responseTickets = booking.getTickets().stream()
                .map(t -> new ConfirmBookingResult.TicketIssued(t.getPassengerId(), t.getTicketNumber()))
                .toList();

        return new ConfirmBookingResult(bookingId, booking.getBookingReference(), "CONFIRMED", responseTickets);
    }
}