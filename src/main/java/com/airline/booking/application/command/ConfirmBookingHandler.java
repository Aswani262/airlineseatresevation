package com.airline.booking.application.command;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.repository.BookingQueryRepository;
import com.airline.booking.repository.BookingRepository;
import com.airline.booking.repository.TicketRepository;
import com.airline.booking.service.SeatInventoryService;
import com.airline.booking.service.TicketingService;
import com.airline.shared.annoation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class ConfirmBookingHandler implements ConfirmBookingUseCase {

    private final BookingQueryRepository bookingQueryRepository;
    private final BookingRepository bookingRepository;
    private final SeatInventoryService seatInventoryService;
    private final TicketRepository ticketRepository;
    private final TicketingService ticketingService;

    @Override
    @Transactional
    public ConfirmBookingResult bookingFinalize(ConfirmBookingCommand command) {

        UUID bookingId = command.getBookingId();
        var snap = bookingQueryRepository.getSnapshot(bookingId);

        // Idempotency: if already confirmed, return current tickets (you can add query for tickets)
        if ("CONFIRMED".equalsIgnoreCase(snap.status())) {
            // minimal: return empty ticket list or fetch tickets via TicketQueryRepository
            return new ConfirmBookingResult(snap.bookingId(), snap.bookingReference(), "CONFIRMED", List.of());
        }

        if (!"DRAFT".equalsIgnoreCase(snap.status())) {
            throw new IllegalStateException("Booking cannot be confirmed in status: " + snap.status());
        }

        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());
        if (snap.holdExpiresAt() != null && now.isAfter(snap.holdExpiresAt())) {
            throw new IllegalStateException("Booking hold expired");
        }

        // 1) Confirm seats in DB: LOCKED -> BOOKED for seats locked by this booking and not expired
        var seatNumbers = snap.seats().stream().map(BookingQueryRepository.BookingSnapshot.SeatLine::seatNumber).toList();
        seatInventoryService.confirmLockedSeatsOrThrow(snap.flightId(), bookingId, seatNumbers);

        // 2) Update booking status DRAFT -> CONFIRMED (CAS update)
        int updated = bookingRepository.updateStatus(bookingId, "DRAFT", "CONFIRMED", now);
        if (updated != 1) {
            throw new IllegalStateException("Booking status changed concurrently; please retry");
        }

        // 3) Issue tickets (one per passenger)
        var tickets = snap.passengers().stream().map(p -> {
            String ticketNo = ticketingService.generateTicketNumber();
            return new TicketRepository.TicketRow(
                    UUID.randomUUID(),
                    ticketNo,
                    bookingId,
                    p.passengerId(),
                    "ISSUED"
            );
        }).toList();

        ticketRepository.insertTickets(tickets);

        var responseTickets = tickets.stream()
                .map(t -> new ConfirmBookingResult.TicketIssued(t.passengerId(), t.ticketNumber()))
                .toList();

        return new ConfirmBookingResult(bookingId, snap.bookingReference(), "CONFIRMED", responseTickets);
    }
}
