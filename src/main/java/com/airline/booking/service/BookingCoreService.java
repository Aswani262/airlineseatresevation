package com.airline.booking.service;


import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.shared.annoation.DomainService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;
//Core domain service that encapsulates the main booking logic, independent of infrastructure concerns
//its middle path between rather putting domain logic in domain objects and having an anemic domain model with all logic in services.
//It can be used by application services to perform booking operations without worrying about the underlying details.
@DomainService
@RequiredArgsConstructor
public class BookingCoreService implements IBookingCoreService {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();

    private static final int REFERENCE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();



    @Override
    public BookingDraft createDraft(BookSeatCommand cmd, int holdMinutes) {

        //TODO:structural validation can be moved to a separate validator class if it grows more complex
        //and its should handle all validation errors and return them in a structured way instead of throwing on the first error
        // use notification pattern for that
        validate(cmd);

        UUID bookingId = UUID.randomUUID();
        String reference = generateBookingRef8();
        OffsetDateTime holdExpiresAt = OffsetDateTime.now(Clock.systemUTC()).plusMinutes(holdMinutes);

        List<String> normalizedSeats = cmd.getSeatSelections().stream()
                .map(s -> normalizeSeat(s.getSeatNumber()))
                .toList();

        BigDecimal totalAmount = cmd.getSeatSelections().stream()
                .map(BookSeatCommand.SeatSelection::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BookingDraft(
                bookingId,
                reference,
                "DRAFT",
                holdExpiresAt,
                totalAmount,
                normalizedSeats
        );
    }

    private void validate(BookSeatCommand cmd) {
        if (cmd == null) throw new IllegalArgumentException("command is required");
        if (cmd.getFlightId() == null) throw new IllegalArgumentException("flightId is required");
        if (cmd.getCustomerId() == null) throw new IllegalArgumentException("customerId is required");

        if (cmd.getCurrency() == null || cmd.getCurrency().isBlank())
            throw new IllegalArgumentException("currency is required");

        if (cmd.getPassengers() == null || cmd.getPassengers().isEmpty())
            throw new IllegalArgumentException("passengers is required");

        if (cmd.getSeatSelections() == null || cmd.getSeatSelections().isEmpty())
            throw new IllegalArgumentException("seatSelections is required");

        int passengerCount = cmd.getPassengers().size();

        Set<String> seenSeats = new HashSet<>();

        for (var sel : cmd.getSeatSelections()) {
            if (sel.getPassengerIndex() == null ||
                    sel.getPassengerIndex() < 0 ||
                    sel.getPassengerIndex() >= passengerCount) {
                throw new IllegalArgumentException("Invalid passengerIndex: " + sel.getPassengerIndex());
            }
            if (sel.getSeatNumber() == null || sel.getSeatNumber().isBlank()) {
                throw new IllegalArgumentException("seatNumber is required");
            }
            if (sel.getPrice() == null || sel.getPrice().signum() < 0) {
                throw new IllegalArgumentException("Invalid seat price");
            }

            String normalized = normalizeSeat(sel.getSeatNumber());
            if (!seenSeats.add(normalized)) {
                throw new IllegalArgumentException("Duplicate seat in request: " + normalized);
            }
        }
    }

    private String normalizeSeat(String seat) {
        return seat.trim().toUpperCase(Locale.ROOT);
    }

    private String generateBookingRef8() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(ALPHANUM.charAt(RND.nextInt(ALPHANUM.length())));
        return sb.toString();
    }



    /**
     * Generate a random 6-character booking reference
     * Format: ABC123
     */
    public String generate() {
        StringBuilder reference = new StringBuilder(REFERENCE_LENGTH);
        for (int i = 0; i < REFERENCE_LENGTH; i++) {
            reference.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return reference.toString();
    }

    public record BookingDraft(
            UUID bookingId,
            String bookingReference,
            String status,
            OffsetDateTime holdExpiresAt,
            BigDecimal totalAmount,
            List<String> seatNumbers
    ) {}
}
