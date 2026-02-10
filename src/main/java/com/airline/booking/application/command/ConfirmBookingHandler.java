package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.domain.model.SeatInventory;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.repository.ISeatInventoryCommandRepository;
import com.airline.booking.service.core.BookingCoreService;
import com.airline.booking.service.core.ISeatInventoryService;
import com.airline.shared.annoation.ApplicationService;
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
    private final BookingCoreService bookingCoreService; // Added to inject the domain service

    @Override
    @Transactional
    public ConfirmBookingResult bookingFinalize(ConfirmBookingCommand command) {

        UUID bookingId = command.getBookingId();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found: " + bookingId));

        BookingStatus originalStatus = booking.getStatus();

        bookingCoreService.confirm(booking); // Invoke domain service to handle status change and invariants (including ticket issuance)

        if (originalStatus == booking.getStatus()) {
            // Already CONFIRMED (idempotent); return with existing tickets
            List<ConfirmBookingResult.TicketIssued> responseTickets = booking.getTickets().stream()
                    .map(t -> new ConfirmBookingResult.TicketIssued(t.getPassengerId(), t.getTicketNumber()))
                    .collect(Collectors.toList());
            return new ConfirmBookingResult(booking.getId(), booking.getBookingReference(), "CONFIRMED", responseTickets);
        }

        // Proceed with confirmation actions (was DRAFT, now CONFIRMED)
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        // 1) Prepare seats
        List<String> seatNumbers = booking.getSeats().stream().map(s -> s.getSeatNumber()).toList();
        List<String> normalized = seatInventoryService.normalizeSeats(seatNumbers);
        List<SeatInventory> seats = seatInventoryRepository.findByFlightIdAndSeatNumberIn(booking.getFlightId(), normalized);

        if (seats.size() != normalized.size()) {
            throw new IllegalStateException("One or more seats not found");
        }

        // Confirm seats in DB: LOCKED -> BOOKED for seats locked by this booking and not expired
        seatInventoryService.confirmLockedSeatsOrThrow(seats, bookingId);

        // Persist updated seats
        try {
            seatInventoryRepository.saveAll(seats);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Seat confirmation failed due to concurrent modification; please retry", e);
        }

        try {
            bookingRepository.save(booking); // Persists changes to booking and children (tickets); optimistic locking via @Version
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Booking status changed concurrently; please retry", e);
        }

        List<ConfirmBookingResult.TicketIssued> responseTickets = booking.getTickets().stream()
                .map(t -> new ConfirmBookingResult.TicketIssued(t.getPassengerId(), t.getTicketNumber()))
                .toList();

        return new ConfirmBookingResult(bookingId, booking.getBookingReference(), "CONFIRMED", responseTickets);
    }
}