package com.airline.flighmngmt.service;


import com.airline.flightmgmt.domain.SeatInventory;
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
        List<SeatInventory> seats = createSeats(2, SeatStatus.AVAILABLE, null, null);

        // Act
        SeatLockResult result = seatInventoryCoreService.lockSeats(seats, bookingId, ttl);

        // Assert
        assertTrue(result.success());
        assertEquals(List.of("A1", "A2"), result.seatNumbers());
        assertNotNull(result.expiresAt());
        assertTrue(result.expiresAt().isAfter(now));

        for (SeatInventory seat : seats) {
            assertEquals(SeatStatus.LOCKED, seat.getStatus());
            assertEquals(bookingId, seat.getLockedByBookingId());
            assertEquals(result.expiresAt(), seat.getLockExpiresAt());
        }
    }

    @Test
    void lockSeats_locksExpiredLockedSeats_success() {
        // Arrange
        List<SeatInventory> seats = createSeats(1, SeatStatus.LOCKED, UUID.randomUUID(), past);

        // Act
        SeatLockResult result = seatInventoryCoreService.lockSeats(seats, bookingId, ttl);

        // Assert
        assertTrue(result.success());
        assertEquals(List.of("A1"), result.seatNumbers());
        assertNotNull(result.expiresAt());

        SeatInventory seat = seats.get(0);
        assertEquals(SeatStatus.LOCKED, seat.getStatus());
        assertEquals(bookingId, seat.getLockedByBookingId());
        assertEquals(result.expiresAt(), seat.getLockExpiresAt());
    }

    @Test
    void lockSeats_failsIfBooked() {
        // Arrange
        List<SeatInventory> seats = createSeats(1, SeatStatus.BOOKED, null, null);

        // Act
        SeatLockResult result = seatInventoryCoreService.lockSeats(seats, bookingId, ttl);

        // Assert
        assertFalse(result.success());
        assertEquals(List.of("A1"), result.seatNumbers());
        assertNotNull(result.expiresAt());

        SeatInventory seat = seats.get(0);
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        assertNull(seat.getLockedByBookingId());
        assertNull(seat.getLockExpiresAt());
    }

    @Test
    void lockSeats_failsIfLockedByOtherNotExpired() {
        // Arrange
        List<SeatInventory> seats = createSeats(1, SeatStatus.LOCKED, UUID.randomUUID(), future);

        // Act
        SeatLockResult result = seatInventoryCoreService.lockSeats(seats, bookingId, ttl);

        // Assert
        assertFalse(result.success());
        SeatInventory seat = seats.get(0);
        assertEquals(SeatStatus.LOCKED, seat.getStatus());
        assertNotEquals(bookingId, seat.getLockedByBookingId());
        assertEquals(future, seat.getLockExpiresAt());
    }

    @Test
    void lockSeats_mixedSuccess() {
        // Arrange
        SeatInventory available = createSeat("A1", SeatStatus.AVAILABLE, null, null);
        SeatInventory booked = createSeat("A2", SeatStatus.BOOKED, null, null);
        List<SeatInventory> seats = List.of(available, booked);

        // Act
        SeatLockResult result = seatInventoryCoreService.lockSeats(seats, bookingId, ttl);

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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> seatInventoryCoreService.lockSeats(new ArrayList<>(), bookingId, ttl));
        assertEquals("seats is required", exception.getMessage());
    }

    @Test
    void confirmLockedSeatsOrThrow_confirmsValidLocks() {
        // Arrange
        List<SeatInventory> seats = createSeats(2, SeatStatus.LOCKED, bookingId, future);

        // Act
        assertDoesNotThrow(() -> seatInventoryCoreService.confirmLockedSeatsOrThrow(seats, bookingId));

        // Assert
        for (SeatInventory seat : seats) {
            assertEquals(SeatStatus.BOOKED, seat.getStatus());
            assertNull(seat.getLockedByBookingId());
            assertNull(seat.getLockExpiresAt());
        }
    }

    @Test
    void confirmLockedSeatsOrThrow_throwsIfNotLockedByBooking() {
        // Arrange
        List<SeatInventory> seats = createSeats(1, SeatStatus.LOCKED, UUID.randomUUID(), future);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> seatInventoryCoreService.confirmLockedSeatsOrThrow(seats, bookingId));
        assertEquals("Seats are not locked by this booking or lock expired", exception.getMessage());
    }

    @Test
    void confirmLockedSeatsOrThrow_throwsIfExpired() {
        // Arrange
        List<SeatInventory> seats = createSeats(1, SeatStatus.LOCKED, bookingId, past);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> seatInventoryCoreService.confirmLockedSeatsOrThrow(seats, bookingId));
        assertEquals("Seats are not locked by this booking or lock expired", exception.getMessage());
    }

    @Test
    void confirmLockedSeatsOrThrow_throwsIfNotLocked() {
        // Arrange
        List<SeatInventory> seats = createSeats(1, SeatStatus.AVAILABLE, null, null);

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
        List<SeatInventory> seats = createSeats(2, SeatStatus.LOCKED, bookingId, future);

        // Act
        assertDoesNotThrow(() -> seatInventoryCoreService.releaseLockedSeatsOrThrow(seats, bookingId));

        // Assert
        for (SeatInventory seat : seats) {
            assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
            assertNull(seat.getLockedByBookingId());
            assertNull(seat.getLockExpiresAt());
        }
    }

    @Test
    void releaseLockedSeatsOrThrow_throwsIfNotLockedByBooking() {
        // Arrange
        List<SeatInventory> seats = createSeats(1, SeatStatus.LOCKED, UUID.randomUUID(), future);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> seatInventoryCoreService.releaseLockedSeatsOrThrow(seats, bookingId));
        assertEquals("Some seats were not locked by this booking (or already released/expired)", exception.getMessage());
    }

    @Test
    void releaseLockedSeatsOrThrow_throwsIfNotLocked() {
        // Arrange
        List<SeatInventory> seats = createSeats(1, SeatStatus.AVAILABLE, null, null);

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
        List<SeatInventory> seats = createSeats(2, SeatStatus.BOOKED, null, null);

        // Act
        seatInventoryCoreService.releaseBookedSeats(seats);

        // Assert
        for (SeatInventory seat : seats) {
            assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        }
    }

    @Test
    void releaseBookedSeats_ignoresOthers() {
        // Arrange
        SeatInventory booked = createSeat("A1", SeatStatus.BOOKED, null, null);
        SeatInventory available = createSeat("A2", SeatStatus.AVAILABLE, null, null);
        SeatInventory locked = createSeat("A3", SeatStatus.LOCKED, bookingId, future);
        List<SeatInventory> seats = List.of(booked, available, locked);

        // Act
        seatInventoryCoreService.releaseBookedSeats(seats);

        // Assert
        assertEquals(SeatStatus.AVAILABLE, booked.getStatus());
        assertEquals(SeatStatus.AVAILABLE, available.getStatus());
        assertEquals(SeatStatus.LOCKED, locked.getStatus());
    }

    private List<SeatInventory> createSeats(int count, SeatStatus status, UUID lockedBy, OffsetDateTime expiresAt) {
        List<SeatInventory> seats = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            seats.add(createSeat("A" + i, status, lockedBy, expiresAt));
        }
        return seats;
    }

    private SeatInventory createSeat(String seatNumber, SeatStatus status, UUID lockedBy, OffsetDateTime expiresAt) {
        return SeatInventory.builder()
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