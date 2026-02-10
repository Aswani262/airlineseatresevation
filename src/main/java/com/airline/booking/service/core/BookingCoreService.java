package com.airline.booking.service.core;

import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.*;
import com.airline.shared.annoation.DomainService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

//Core domain service that encapsulates the main booking logic, independent of infrastructure concerns
//its middle path between rather putting domain logic in domain objects and having an anemic domain model with all logic in services.
//It can be used by application services to perform booking operations without worrying about the underlying details.
@DomainService
@RequiredArgsConstructor
public class BookingCoreService implements IBookingService {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();
    private final TicketingCoreService ticketingService; // Injected if separate; assuming it's now part of this or separate

    @Override
    public Booking createDraft(BookSeatCommand cmd) {

        //TODO:structural validation can be moved to a separate validator class if it grows more complex
        //and its should handle all validation errors and return them in a structured way instead of throwing on the first error
        // use notification pattern for that
        validate(cmd);

        UUID bookingId = UUID.randomUUID();
        String reference = generateBookingRef8();

        List<Passenger> passengers = new ArrayList<>();
        List<UUID> passengerIds = new ArrayList<>();

        for (var p : cmd.getPassengers()) {
            UUID passengerId = UUID.randomUUID();
            passengerIds.add(passengerId);
            passengers.add(Passenger.builder()
                    .id(passengerId)
                    .bookingId(bookingId)
                    .firstName(p.getFirstName().trim())
                    .lastName(p.getLastName().trim())
                    .email(p.getEmail())
                    .phone(p.getPhone())
                    .passportNumber(p.getPassportNumber())
                    .dateOfBirth(null) // Can be added if needed for age-based pricing or other rules
                    .passengerType(PassengerType.valueOf(p.getPassengerType().trim().toUpperCase(Locale.ROOT)))
                    .build());
        }

        List<BookingSeat> seats = new ArrayList<>();
        for (var sel : cmd.getSeatSelections()) {
            UUID passengerId = passengerIds.get(sel.getPassengerIndex());
            String normalizedSeat = normalizeSeat(sel.getSeatNumber());

            seats.add(BookingSeat.builder()
                    .bookingId(bookingId)
                    .passengerId(passengerId)
                    .seatNumber(normalizedSeat)
                    .fareClass(FareClass.valueOf(sel.getFareClass().trim().toUpperCase(Locale.ROOT)))
                    .price(sel.getPrice())
                    .build());
        }

        BigDecimal totalAmount = seats.stream()
                .map(BookingSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Booking.builder()
                .id(bookingId)
                .bookingReference(reference)
                .flightId(cmd.getFlightId())
                .customerId(cmd.getCustomerId())
                .totalAmount(totalAmount)
                .currency(cmd.getCurrency().trim().toUpperCase(Locale.ROOT))
                .status(BookingStatus.DRAFT)
                .bookingDate(OffsetDateTime.now(Clock.systemUTC()))
                .holdExpiresAt(null)
                .passengers(passengers)
                .seats(seats)
                .tickets(new ArrayList<>())
                .build();
    }

    // New method added for cancel logic (validation and state change)
    @Override
    public void cancel(Booking booking) {
        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.CANCELLED || current == BookingStatus.EXPIRED) {
            return; // Idempotent, no change
        }
        if (current != BookingStatus.DRAFT && current != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel in status: " + current);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        // Cancel tickets if CONFIRMED
        if (current == BookingStatus.CONFIRMED) {
            booking.getTickets().forEach(ticket -> {
                if (ticket.getStatus() == TicketStatus.ISSUED) {
                    ticket.setStatus(TicketStatus.CANCELLED);
                }
            });
        }
    }

    // New method added for confirm logic (validation and state change)
    public void confirm(Booking booking) {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());
        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.CONFIRMED) {
            return; // Idempotent, no change
        }
        if (current != BookingStatus.DRAFT) {
            throw new IllegalStateException("Cannot confirm in status: " + current);
        }
        if (booking.getHoldExpiresAt() != null && now.isAfter(booking.getHoldExpiresAt())) {
            throw new IllegalStateException("Booking hold expired");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        // Issue tickets
        List<Ticket> tickets = booking.getPassengers().stream()
                .map(p -> Ticket.builder()
                        .ticketNumber(ticketingService.generateTicketNumber())
                        .bookingId(booking.getId())
                        .passengerId(p.getId())
                        .status(TicketStatus.ISSUED)
                        .issuedAt(now)
                        .build())
                .collect(Collectors.toList());
        booking.setTickets(tickets);
    }



    private String normalizeSeat(String seat) {
        return seat.trim().toUpperCase(Locale.ROOT);
    }

    private String generateBookingRef8() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(ALPHANUM.charAt(RND.nextInt(ALPHANUM.length())));
        return sb.toString();
    }

    public void validate(BookSeatCommand cmd) {
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
}