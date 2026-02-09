package com.airline.booking.service;


import com.airline.booking.repository.SeatInventoryRepository;
import com.airline.shared.annoation.DomainService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@DomainService
@RequiredArgsConstructor
public class SeatInventoryService implements ISeatInventoryService {

    private final SeatInventoryRepository seatInventoryRepository;

    @Override
    public SeatLockResult lockSeats(UUID flightId, UUID bookingId, List<String> seatNumbers, Duration ttl) {

        List<String> normalized = normalizeSeats(seatNumbers);
        int updated = seatInventoryRepository.lockSeats(flightId, normalized, bookingId, ttl);

        boolean success = updated == normalized.size();
        OffsetDateTime expiresAt = OffsetDateTime.now(Clock.systemUTC()).plusSeconds(ttl.getSeconds());

        return new SeatLockResult(success, normalized, expiresAt);
    }

    @Override
    public void ensureLockedOrThrow(SeatLockResult result) {
        if (!result.success()) {
            throw new IllegalStateException("One or more seats are not available");
        }
    }

    private List<String> normalizeSeats(List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("seatNumbers is required");
        }
        return seatNumbers.stream()
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    @Override
    public void confirmLockedSeatsOrThrow(UUID flightId, UUID bookingId, List<String> seatNumbers) {
        List<String> normalized = seatNumbers.stream()
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();

        int updated = seatInventoryRepository.confirmSeats(flightId, normalized, bookingId);
        if (updated != normalized.size()) {
            throw new IllegalStateException("Seats are not locked by this booking or lock expired");
        }
    }

    @Override
    public void releaseLockedSeatsOrThrow(UUID flightId, UUID bookingId, List<String> seatNumbers) {
        List<String> normalized = normalize(seatNumbers);

        int updated = seatInventoryRepository.releaseLockedSeats(flightId, normalized, bookingId);
        if (updated != normalized.size()) {
            throw new IllegalStateException("Some seats were not locked by this booking (or already released/expired)");
        }
    }

    @Override
    public void releaseBookedSeats(UUID flightId, List<String> seatNumbers) {
        // For confirmed booking cancellation
        List<String> normalized = normalize(seatNumbers);

        int updated = seatInventoryRepository.releaseBookedSeats(flightId, normalized);
        // We don't throw here because it can be idempotent (seat might already be available)
        // But you can enforce updated == size if you want strictness.
    }

    private List<String> normalize(List<String> seatNumbers) {
        return seatNumbers.stream()
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    public record SeatLockResult(
            boolean success,
            List<String> seatNumbers,
            OffsetDateTime expiresAt
    ) {}
}
