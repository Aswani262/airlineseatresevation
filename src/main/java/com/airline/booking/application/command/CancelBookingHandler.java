package com.airline.booking.application.command;

import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.airline.booking.repository.BookingQueryRepository;
import com.airline.booking.repository.BookingRepository;
import com.airline.booking.repository.TicketRepository;
import com.airline.booking.service.SeatInventoryService;
import com.airline.shared.annoation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class CancelBookingHandler implements CancelBookingUseCase {

    private final BookingQueryRepository bookingQueryRepository;
    private final BookingRepository bookingRepository;
    private final SeatInventoryService seatInventoryService;
    private final TicketRepository ticketRepository;


    @Override
    @Transactional
    public CancelBookingResult cancel(CancelBookingCommand command) {

        UUID bookingId = command.getBookingId();
        var snap = bookingQueryRepository.getSnapshot(bookingId);

        // Idempotency
        if ("CANCELLED".equalsIgnoreCase(snap.status())) {
            return new CancelBookingResult(snap.bookingId(), snap.bookingReference(), "CANCELLED");
        }

        if ("EXPIRED".equalsIgnoreCase(snap.status())) {
            return new CancelBookingResult(snap.bookingId(), snap.bookingReference(), "EXPIRED");
        }

        var now = OffsetDateTime.now(Clock.systemUTC());
        List<String> seatNumbers = snap.seats().stream().map(s -> s.seatNumber()).toList();

        if ("DRAFT".equalsIgnoreCase(snap.status())) {
            // release only seats locked by THIS booking
            seatInventoryService.releaseLockedSeatsOrThrow(snap.flightId(), bookingId, seatNumbers);

            int updated = bookingRepository.updateStatus(bookingId, "DRAFT", "CANCELLED", now);
            if (updated != 1) {
                throw new IllegalStateException("Booking status changed concurrently; please retry");
            }

            return new CancelBookingResult(bookingId, snap.bookingReference(), "CANCELLED");
        }

        if ("CONFIRMED".equalsIgnoreCase(snap.status())) {
            // Cancel tickets first (or after; within tx is fine)
            ticketRepository.cancelTicketsByBooking(bookingId);

            // return seats to inventory (policy decision)
            seatInventoryService.releaseBookedSeats(snap.flightId(), seatNumbers);

            int updated = bookingRepository.updateStatus(bookingId, "CONFIRMED", "CANCELLED", now);
            if (updated != 1) {
                // If someone already cancelled concurrently, treat as idempotent on retry
                throw new IllegalStateException("Booking status changed concurrently; please retry");
            }

            return new CancelBookingResult(bookingId, snap.bookingReference(), "CANCELLED");
        }

        throw new IllegalStateException("Booking cannot be cancelled in status: " + snap.status());
    }
}
