package com.airline.booking.service;

import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.domain.model.FareClassCode;
import com.airline.booking.domain.model.Passenger;
import com.airline.booking.domain.model.PassengerType;
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

//Core domain service that encapsulates the main booking logic, independent of infrastructure concerns
//its middle path between rather putting domain logic in domain objects and having an anemic domain model with all logic in services.
//It can be used by application services to perform booking operations without worrying about the underlying details.
@DomainService
@RequiredArgsConstructor
public class BookingCoreService implements IBookingCoreService {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();

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
                    .dateOfBirth(null)  // Not provided in command; set to null or handle accordingly
                    .passengerType(PassengerType.valueOf(p.getPassengerType().trim().toUpperCase(Locale.ROOT)))
                    .build());
        }

        List<BookingSeat> seats = new ArrayList<>();
        for (var sel : cmd.getSeatSelections()) {
            UUID bookingSeatId = UUID.randomUUID();
            UUID passengerId = passengerIds.get(sel.getPassengerIndex());
            String normalizedSeat = normalizeSeat(sel.getSeatNumber());

            seats.add(BookingSeat.builder()
                    .id(bookingSeatId)
                    .bookingId(bookingId)
                    .passengerId(passengerId)
                    .seatNumber(normalizedSeat)
                    .fareClass(new FareClassCode(sel.getFareClass().trim().toUpperCase(Locale.ROOT)))
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
                .holdExpiresAt(null)  // Set after seat locking
                .passengers(passengers)
                .seats(seats)
                .tickets(new ArrayList<>())
                .build();
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
}