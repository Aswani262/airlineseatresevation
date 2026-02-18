package com.airline.flighmngmt.application;

import com.airline.flightmgmt.application.command.LockSeatCommand;
import com.airline.flightmgmt.application.command.LockSeatHandler;
import com.airline.flightmgmt.domain.FareClass;
import com.airline.flightmgmt.domain.SeatInventory;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.exception.SeatLockingFailedException;
import com.airline.flightmgmt.exception.SeatNotAvailableException;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.flightmgmt.service.ISeatInventoryService;
import com.airline.shared.model.SeatLockResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockSeatHandlerTest {

    @Mock
    private ISeatInventoryCommandRepository seatInventoryRepository;

    @Mock
    private ISeatInventoryService seatInventoryService;

    @InjectMocks
    private LockSeatHandler handler;

    @Test
    void lockSeat_success_allSeatsAvailable_shouldNormalize_find_lock_saveAndReturnResult() {
        // Given
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        List<String> originalSeats = List.of("12a", "14b");
        List<String> normalizedSeats = List.of("12A", "14B");

        LockSeatCommand command = new LockSeatCommand(flightId, originalSeats, bookingId, 10);

        List<SeatInventory> foundSeats = List.of(
                createAvailableSeat(flightId, "12A"),
                createAvailableSeat(flightId, "14B")
        );

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(10);
        SeatLockResult lockResult = new SeatLockResult(true, normalizedSeats, expiresAt);

        when(seatInventoryService.normalizeSeats(originalSeats)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndSeatNumberIn(flightId, normalizedSeats))
                .thenReturn(foundSeats);
        when(seatInventoryService.lockSeats(foundSeats, bookingId, Duration.ofMinutes(10)))
                .thenReturn(lockResult);

        // When
        SeatLockResult result = handler.lockSeat(command);

        // Then
        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(normalizedSeats, result.seatNumbers());
        assertEquals(expiresAt, result.expiresAt());

        // Verify orchestration
        verify(seatInventoryService).normalizeSeats(originalSeats);

        ArgumentCaptor<List<String>> findCaptor = ArgumentCaptor.forClass(List.class);
        verify(seatInventoryRepository).findByFlightIdAndSeatNumberIn(eq(flightId), findCaptor.capture());
        assertEquals(normalizedSeats, findCaptor.getValue());

        ArgumentCaptor<List<SeatInventory>> lockCaptor = ArgumentCaptor.forClass(List.class);
        verify(seatInventoryService).lockSeats(lockCaptor.capture(), eq(bookingId), eq(Duration.ofMinutes(10)));
        assertSame(foundSeats, lockCaptor.getValue());

        verify(seatInventoryRepository).saveAll(foundSeats);
    }

    @Test
    void lockSeat_notAllSeatsLockable_throwsSeatNotAvailableException_andDoesNotSave() {
        // Given
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        List<String> originalSeats = List.of("12a");
        List<String> normalizedSeats = List.of("12A");

        LockSeatCommand command = new LockSeatCommand(flightId, originalSeats, bookingId, 10);

        List<SeatInventory> foundSeats = List.of(createAvailableSeat(flightId, "12A"));

        SeatLockResult failedLock = new SeatLockResult(false, normalizedSeats, OffsetDateTime.now().plusMinutes(10));

        when(seatInventoryService.normalizeSeats(originalSeats)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndSeatNumberIn(flightId, normalizedSeats))
                .thenReturn(foundSeats);
        when(seatInventoryService.lockSeats(foundSeats, bookingId, Duration.ofMinutes(10)))
                .thenReturn(failedLock);

        // When & Then
        SeatNotAvailableException ex = assertThrows(
                SeatNotAvailableException.class,
                () -> handler.lockSeat(command)
        );

        // Exception message typically contains seat numbers (as per common pattern)
        assertTrue(ex.getMessage().contains("12A") || ex.getMessage().contains("12a"));

        verify(seatInventoryRepository, never()).saveAll(any());
    }

    @Test
    void lockSeat_optimisticLockingFailureOnSave_throwsSeatLockingFailedException() {
        // Given
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        List<String> originalSeats = List.of("1A");
        List<String> normalizedSeats = List.of("1A");

        LockSeatCommand command = new LockSeatCommand(flightId, originalSeats, bookingId, 10);

        List<SeatInventory> foundSeats = List.of(createAvailableSeat(flightId, "1A"));

        SeatLockResult successLock = new SeatLockResult(true, normalizedSeats, OffsetDateTime.now().plusMinutes(10));

        when(seatInventoryService.normalizeSeats(originalSeats)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndSeatNumberIn(flightId, normalizedSeats))
                .thenReturn(foundSeats);
        when(seatInventoryService.lockSeats(foundSeats, bookingId, Duration.ofMinutes(10)))
                .thenReturn(successLock);

        doThrow(new OptimisticLockingFailureException("Version conflict"))
                .when(seatInventoryRepository).saveAll(any());

        // When & Then
        SeatLockingFailedException ex = assertThrows(
                SeatLockingFailedException.class,
                () -> handler.lockSeat(command)
        );


        verify(seatInventoryRepository).saveAll(foundSeats);
    }


    // ==================== Helper ====================

    private SeatInventory createAvailableSeat(UUID flightId, String seatNumber) {
        return SeatInventory.builder()
                .id(UUID.randomUUID())
                .flightId(flightId)
                .seatNumber(seatNumber)
                .fareClass(FareClass.ECONOMY)
                .status(SeatStatus.AVAILABLE)
                .lockedByBookingId(null)
                .lockExpiresAt(null)
                .price(BigDecimal.valueOf(5000))
                .build();
    }
}