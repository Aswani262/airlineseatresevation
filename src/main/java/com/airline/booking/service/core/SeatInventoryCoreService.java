package com.airline.booking.service.core;

import com.airline.booking.domain.model.SeatInventory;
import com.airline.booking.domain.model.SeatStatus;
import com.airline.shared.annoation.DomainService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@DomainService
@RequiredArgsConstructor
public class SeatInventoryCoreService implements ISeatInventoryService {

    @Override
    public SeatLockResult lockSeats(List<SeatInventory> seats, UUID bookingId, Duration ttl) {
        // Assume seats are already normalized and loaded by caller
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("seats is required");
        }

        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());
        OffsetDateTime expiresAt = now.plusSeconds(ttl.getSeconds());

        int successCount = 0;
        for (SeatInventory seat : seats) {
            if (canLock(seat, now)) {
                seat.setStatus(SeatStatus.LOCKED);
                seat.setLockedByBookingId(bookingId);
                seat.setLockExpiresAt(expiresAt);
                successCount++;
            }
        }

        boolean success = successCount == seats.size();
        List<String> seatNumbers = seats.stream().map(SeatInventory::getSeatNumber).collect(Collectors.toList());
        return new SeatLockResult(success, seatNumbers, expiresAt);
    }

    private boolean canLock(SeatInventory seat, OffsetDateTime now) {
        return seat.getStatus() == SeatStatus.AVAILABLE ||
               (seat.getStatus() == SeatStatus.LOCKED && (seat.getLockExpiresAt() == null || now.isAfter(seat.getLockExpiresAt())));
    }

    @Override
    public void confirmLockedSeatsOrThrow(List<SeatInventory> seats, UUID bookingId) {
        // Assume seats are already loaded by caller
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("seats is required");
        }

        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        int confirmedCount = 0;
        for (SeatInventory seat : seats) {
            if (seat.getStatus() == SeatStatus.LOCKED &&
                seat.getLockedByBookingId().equals(bookingId) &&
                (seat.getLockExpiresAt() == null || !now.isAfter(seat.getLockExpiresAt()))) {
                seat.setStatus(SeatStatus.BOOKED);
                seat.setLockedByBookingId(null);
                seat.setLockExpiresAt(null);
                confirmedCount++;
            }
        }

        if (confirmedCount != seats.size()) {
            throw new IllegalStateException("Seats are not locked by this booking or lock expired");
        }
    }

    @Override
    public void releaseLockedSeatsOrThrow(List<SeatInventory> seats, UUID bookingId) {
        // Assume seats are already loaded by caller
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("seats is required");
        }

        int updatedCount = 0;
        for (SeatInventory seat : seats) {
            if (seat.getStatus() == SeatStatus.LOCKED && seat.getLockedByBookingId().equals(bookingId)) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLockedByBookingId(null);
                seat.setLockExpiresAt(null);
                updatedCount++;
            }
        }

        if (updatedCount != seats.size()) {
            throw new IllegalStateException("Some seats were not locked by this booking (or already released/expired)");
        }
    }

    @Override
    public void releaseBookedSeats(List<SeatInventory> seats) {
        // For confirmed booking cancellation
        // Assume seats are already loaded by caller

        // Idempotent: only release if BOOKED
        for (SeatInventory seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                seat.setStatus(SeatStatus.AVAILABLE);
            }
        }
    }

    @Override
    public List<String> normalizeSeats(List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("seatNumbers is required");
        }
        return seatNumbers.stream()
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    public record SeatLockResult(
            boolean success,
            List<String> seatNumbers,
            OffsetDateTime expiresAt
    ) {}
}