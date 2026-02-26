package com.airline.flighmngmt.service;


import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.service.SeatInventoryCoreService;
import com.airline.flightmgmt.domain.FareClass;
import com.airline.shared.model.SeatLockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SeatInventoryCoreServiceTest {

    private SeatInventoryCoreService seatInventoryCoreService;

    private UUID bookingId;
    private Duration ttl;
    private OffsetDateTime now;
    private OffsetDateTime future;
    private OffsetDateTime past;

    @BeforeEach
    void setUp() {
        seatInventoryCoreService = new SeatInventoryCoreService();
        bookingId = UUID.randomUUID();
        ttl = Duration.ofMinutes(10);
        now = OffsetDateTime.now();
        future = now.plusMinutes(15);
        past = now.minusMinutes(5);
    }

    @Test
    void normalizeSeats_normalizesCorrectly() {
        // Arrange
        List<String> seatNumbers = List.of(" a1 ", "B2", "c3 ");

        // Act
        List<String> result = seatInventoryCoreService.normalizeSeats(seatNumbers);

        // Assert
        assertEquals(List.of("A1", "B2", "C3"), result);
    }

    @Test
    void normalizeSeats_throwsIfNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> seatInventoryCoreService.normalizeSeats(null));
        assertEquals("seatNumbers is required", exception.getMessage());
    }

    @Test
    void normalizeSeats_throwsIfEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> seatInventoryCoreService.normalizeSeats(Collections.emptyList()));
        assertEquals("seatNumbers is required", exception.getMessage());
    }

    @Test
    void lockSeats_locksAvailableSeats_success() {
        // Arrange
        List<SeatAssignments> seats = createSeats(2, SeatStatus.AVAILABLE, null, null);

        // Act
        SeatLockResult result = seatInventoryCoreService.holdSeats(seats, bookingId, ttl);

        // Assert
        assertTrue(result.success());
        assertEquals(List.of("A1", "A2"), result.seatNumbers());
        assertNotNull(result.expiresAt());
        assertTrue(result.expiresAt().isAfter(now));

        for (SeatAssignments seat : seats) {
            assertEquals(SeatStatus.LOCKED, seat.getStatus());
            assertEquals(bookingId, seat.getLockedByBookingId());
            assertEquals(result.expiresAt(), seat.getLockExpiresAt());
        }
    }

    @Test
    void lockSeats_locksExpiredLockedSeats_success() {
        // Arrange
        List<SeatAssignments> seats = createSeats(1, SeatStatus.LOCKED, UUID.randomUUID(), past);

        // Act
        SeatLockResult result = seatInventoryCoreService.holdSeats(seats, bookingId, ttl);

        // Assert
        assertTrue(result.success());
        assertEquals(List.of("A1"), result.seatNumbers());
        assertNotNull(result.expiresAt());

        SeatAssignments seat = seats.get(0);
        assertEquals(SeatStatus.LOCKED, seat.getStatus());
        assertEquals(bookingId, seat.getLockedByBookingId());
        assertEquals(result.expiresAt(), seat.getLockExpiresAt());
    }

    @Test
    void lockSeats_failsIfBooked() {
        // Arrange
        List<SeatAssignments> seats = createSeats(1, SeatStatus.BOOKED, null, null);

        // Act
        SeatLockResult result = seatInventoryCoreService.holdSeats(seats, bookingId, ttl);

        // Assert
        assertFalse(result.success());
        assertEquals(List.of("A1"), result.seatNumbers());
        assertNotNull(result.expiresAt());

        SeatAssignments seat = seats.get(0);
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        assertNull(seat.getLockedByBookingId());
        assertNull(seat.getLockExpiresAt());
    }

    @Test
    void lockSeats_failsIfLockedByOtherNotExpired() {
        // Arrange
        List<SeatAssignments> seats = createSeats(1, SeatStatus.LOCKED, UUID.randomUUID(), future);

        // Act
        SeatLockResult result = seatInventoryCoreService.holdSeats(seats, bookingId, ttl);

        // Assert
        assertFalse(result.success());
        SeatAssignments seat = seats.get(0);
        assertEquals(SeatStatus.LOCKED, seat.getStatus());
        assertNotEquals(bookingId, seat.getLockedByBookingId());
        assertEquals(future, seat.getLockExpiresAt());
    }

    @Test
    void lockSeats_mixedSuccess() {
        // Arrange
        SeatAssignments available = createSeat("A1", SeatStatus.AVAILABLE, null, null);
        SeatAssignments booked = createSeat("A2", SeatStatus.BOOKED, null, null);
        List<SeatAssignments> seats = List.of(available, booked);

        // Act
        SeatLockResult result = seatInventoryCoreService.holdSeats(seats, bookingId, ttl);

        // Assert
        assertFalse(result.success());
        assertEquals(List.of("A1", "A2"), result.seatNumbers());

        assertEquals(SeatStatus.LOCKED, available.getStatus());
        assertEquals(bookingId, available.getLockedByBookingId());
        assertNotNull(available.getLockExpiresAt());

        assertEquals(SeatStatus.BOOKED, booked.getStatus());
        assertNull(booked.getLockedByBookingId());
        assertNull(booked.getLockExpiresAt());
    }

    @Test
    void lockSeats_throwsIfEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> seatInventoryCoreService.holdSeats(new ArrayList<>(), bookingId, ttl));
        assertEquals("seats is required", exception.getMessage());
    }

    @Test
    void confirmLockedSeatsOrThrow_confirmsValidLocks() {
        // Arrange
        List<SeatAssignments> seats = createSeats(2, SeatStatus.LOCKED, bookingId, future);

        // Act
        assertDoesNotThrow(() -> seatInventoryCoreService.confirmLockedSeatsOrThrow(seats, bookingId));

        // Assert
        for (SeatAssignments seat : seats) {
            assertEquals(SeatStatus.BOOKED, seat.getStatus());
            assertNull(seat.getLockedByBookingId());
            assertNull(seat.getLockExpiresAt());
        }
    }

    @Test
    void confirmLockedSeatsOrThrow_throwsIfNotLockedByBooking() {
        // Arrange
        List<SeatAssignments> seats = createSeats(1, SeatStatus.LOCKED, UUID.randomUUID(), future);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> seatInventoryCoreService.confirmLockedSeatsOrThrow(seats, bookingId));
        assertEquals("Seats are not locked by this booking or lock expired", exception.getMessage());
    }

    @Test
    void confirmLockedSeatsOrThrow_throwsIfExpired() {
        // Arrange
        List<SeatAssignments> seats = createSeats(1, SeatStatus.LOCKED, bookingId, past);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> seatInventoryCoreService.confirmLockedSeatsOrThrow(seats, bookingId));
        assertEquals("Seats are not locked by this booking or lock expired", exception.getMessage());
    }

    @Test
    void confirmLockedSeatsOrThrow_throwsIfNotLocked() {
        // Arrange
        List<SeatAssignments> seats = createSeats(1, SeatStatus.AVAILABLE, null, null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> seatInventoryCoreService.confirmLockedSeatsOrThrow(seats, bookingId));
        assertEquals("Seats are not locked by this booking or lock expired", exception.getMessage());
    }

    @Test
    void confirmLockedSeatsOrThrow_throwsIfEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> seatInventoryCoreService.confirmLockedSeatsOrThrow(new ArrayList<>(), bookingId));
        assertEquals("seats is required", exception.getMessage());
    }

    @Test
    void releaseLockedSeatsOrThrow_releasesValidLocks() {
        // Arrange
        List<SeatAssignments> seats = createSeats(2, SeatStatus.LOCKED, bookingId, future);

        // Act
        assertDoesNotThrow(() -> seatInventoryCoreService.releaseLockedSeatsOrThrow(seats, bookingId));

        // Assert
        for (SeatAssignments seat : seats) {
            assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
            assertNull(seat.getLockedByBookingId());
            assertNull(seat.getLockExpiresAt());
        }
    }

    @Test
    void releaseLockedSeatsOrThrow_throwsIfNotLockedByBooking() {
        // Arrange
        List<SeatAssignments> seats = createSeats(1, SeatStatus.LOCKED, UUID.randomUUID(), future);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> seatInventoryCoreService.releaseLockedSeatsOrThrow(seats, bookingId));
        assertEquals("Some seats were not locked by this booking (or already released/expired)", exception.getMessage());
    }

    @Test
    void releaseLockedSeatsOrThrow_throwsIfNotLocked() {
        // Arrange
        List<SeatAssignments> seats = createSeats(1, SeatStatus.AVAILABLE, null, null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> seatInventoryCoreService.releaseLockedSeatsOrThrow(seats, bookingId));
        assertEquals("Some seats were not locked by this booking (or already released/expired)", exception.getMessage());
    }

    @Test
    void releaseLockedSeatsOrThrow_throwsIfEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> seatInventoryCoreService.releaseLockedSeatsOrThrow(new ArrayList<>(), bookingId));
        assertEquals("seats is required", exception.getMessage());
    }

    @Test
    void releaseBookedSeats_releasesBooked() {
        // Arrange
        List<SeatAssignments> seats = createSeats(2, SeatStatus.BOOKED, null, null);

        // Act
        seatInventoryCoreService.releaseBookedSeats(seats);

        // Assert
        for (SeatAssignments seat : seats) {
            assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        }
    }

    @Test
    void releaseBookedSeats_ignoresOthers() {
        // Arrange
        SeatAssignments booked = createSeat("A1", SeatStatus.BOOKED, null, null);
        SeatAssignments available = createSeat("A2", SeatStatus.AVAILABLE, null, null);
        SeatAssignments locked = createSeat("A3", SeatStatus.LOCKED, bookingId, future);
        List<SeatAssignments> seats = List.of(booked, available, locked);

        // Act
        seatInventoryCoreService.releaseBookedSeats(seats);

        // Assert
        assertEquals(SeatStatus.AVAILABLE, booked.getStatus());
        assertEquals(SeatStatus.AVAILABLE, available.getStatus());
        assertEquals(SeatStatus.LOCKED, locked.getStatus());
    }

    private List<SeatAssignments> createSeats(int count, SeatStatus status, UUID lockedBy, OffsetDateTime expiresAt) {
        List<SeatAssignments> seats = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            seats.add(createSeat("A" + i, status, lockedBy, expiresAt));
        }
        return seats;
    }

    private SeatAssignments createSeat(String seatNumber, SeatStatus status, UUID lockedBy, OffsetDateTime expiresAt) {
        return SeatAssignments.builder()
                .id(UUID.randomUUID())
                .flightId(UUID.randomUUID())
                .seatNumber(seatNumber)
                .fareClass(FareClass.ECONOMY)
                .status(status)
                .lockedByBookingId(lockedBy)
                .lockExpiresAt(expiresAt)
                .price(BigDecimal.valueOf(100))
                .build();
    }
}