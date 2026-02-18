package com.airline.booking.service.core;

import com.airline.booking.application.command.dto.InitiateBookingSeatCommand;
import com.airline.booking.domain.model.*;
import com.airline.booking.exception.BookingHoldExpiredException;
import com.airline.booking.exception.IllegaBookingStatus;
import com.airline.shared.annotation.CoreService;
import com.airline.shared.exception.ErrorNotification;
import com.airline.shared.exception.StructuralException;
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

//Core domain service that encapsulates the main booking logic(the logic which is going to part of booking aggregate), independent of infrastructure concerns
//its middle path between rather putting domain logic in domain objects and having an anemic domain model with all logic in services.
//It can be used by application services to perform booking operations without worrying about the underlying details.
@CoreService
@RequiredArgsConstructor
public class BookingCoreService implements IBookingService {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();
    private final TicketingCoreService ticketingService;

    @Override
    public Booking createDraft(InitiateBookingSeatCommand cmd) {

        //TODO:structural validation can be moved to a separate validator class if it grows more complex
        // use notification pattern for that
        ErrorNotification notification =  validate(cmd);

        if(notification.hasErrors()){
            throw new StructuralException(notification);
        }

        //We can move this to Aggregate factory if we want to keep service class thin
        //And also if we want to reuse the booking creation logic in other places,
        // but for simplicity keeping it here for now as its only used in one place and not too complex
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

    public void confirm(Booking booking) {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());
        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.CONFIRMED) {
            return; // Idempotent, no change
        }
        if (current != BookingStatus.DRAFT) {
            throw new IllegaBookingStatus("Cannot confirm booking in status: " + current);
        }
        if (booking.getHoldExpiresAt() != null && now.isAfter(booking.getHoldExpiresAt())) {
            throw new BookingHoldExpiredException();
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

    public ErrorNotification validate(InitiateBookingSeatCommand cmd) {
        ErrorNotification notification = new ErrorNotification();
        if (cmd == null) {
            notification.add("command_required", "command", "command is required");
            return notification;
        }

        if (cmd.getFlightId() == null) {
            notification.add("flight_id_required", "flightId", "flightId is required");
        }

        if (cmd.getCustomerId() == null) {
            notification.add("customer_id_required", "customerId", "customerId is required");
        }

        if (cmd.getCurrency() == null || cmd.getCurrency().isBlank()) {
            notification.add("currency_required", "currency", "currency is required");
        }

        if (cmd.getPassengers() == null || cmd.getPassengers().isEmpty()) {
            notification.add("passengers_required", "passengers", "passengers is required");
        }

        if (cmd.getSeatSelections() == null || cmd.getSeatSelections().isEmpty()) {
            notification.add("seat_selections_required", "seatSelections", "seatSelections is required");
        } else if (cmd.getPassengers() != null && cmd.getSeatSelections().size() != cmd.getPassengers().size()) {
            notification.add("seat_selections_mismatch", "seatSelections", "Number of seat selections must match number of passengers");
        }

        if (cmd.getPassengers() != null) {
            int passengerCount = cmd.getPassengers().size();

            Set<String> seenSeats = new HashSet<>();

            if (cmd.getSeatSelections() != null) {
                for (var sel : cmd.getSeatSelections()) {
                    if (sel.getPassengerIndex() == null ||
                            sel.getPassengerIndex() < 0 ||
                            sel.getPassengerIndex() >= passengerCount) {
                        notification.add("invalid_passenger_index", "passengerIndex", "Invalid passengerIndex: " + sel.getPassengerIndex());
                    }
                    if (sel.getSeatNumber() == null || sel.getSeatNumber().isBlank()) {
                        notification.add("seat_number_required", "seatNumber", "seatNumber is required");
                    }
                    if (sel.getPrice() == null || sel.getPrice().signum() < 0) {
                        notification.add("invalid_seat_price", "price", "Invalid seat price");
                    }

                    if (sel.getSeatNumber() != null) {
                        String normalized = normalizeSeat(sel.getSeatNumber());
                        if (!seenSeats.add(normalized)) {
                            notification.add("duplicate_seat", "seatNumber", "Duplicate seat in request: " + normalized);
                        }
                    }
                }
            }
        }
        return notification;
    }
}