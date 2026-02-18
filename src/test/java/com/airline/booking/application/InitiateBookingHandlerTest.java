package com.airline.booking.application;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.InitiateBookingHandler;
import com.airline.booking.application.command.dto.InitiateBookingSeatCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.shared.model.SeatLockResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiateBookingHandlerTest {

    @Mock
    private IBookingService bookingCoreService;

    @Mock
    private IBookingCommandRepository bookingRepository;

    @Mock
    private SeatInventoryIntegrationService seatInventoryService;

    @InjectMocks
    private InitiateBookingHandler handler;

    private static final int HOLD_MINUTES = 10;

    @Test
    void initiateBooking_success_shouldCreateDraft_lockSeats_setHoldExpiration_persistAndReturnResult() {
        // Given
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String bookingRef = "ABCDEF12";
        OffsetDateTime holdExpiresAt = OffsetDateTime.now().plusMinutes(15);
        String seatNumber = "12A";

        // Real command (with valid data)
        InitiateBookingSeatCommand command = createValidCommand(flightId, customerId, seatNumber);

        // Booking returned by core service
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status(BookingStatus.DRAFT)
                .seats(List.of(
                        BookingSeat.builder()
                                .seatNumber(seatNumber)
                                .build()
                ))
                .build();

        SeatLockResult lockResult = new SeatLockResult(true, List.of(seatNumber), holdExpiresAt);

        when(bookingCoreService.createDraft(command)).thenReturn(booking);
        when(seatInventoryService.lockSeats(eq(flightId), anyList(), eq(bookingId), eq(HOLD_MINUTES)))
                .thenReturn(lockResult);

        // When
        BookSeatResult result = handler.initiateBooking(command);

        // Then
        assertNotNull(result);
        assertEquals(bookingId, result.bookingId());
        assertEquals(bookingRef, result.bookingReference());
        assertEquals("DRAFT", result.status());
        assertEquals(holdExpiresAt, result.holdExpiresAt());

        // Verify seat locking was called with correct seat list from booking
        ArgumentCaptor<List<String>> seatsCaptor = ArgumentCaptor.forClass(List.class);
        verify(seatInventoryService).lockSeats(
                eq(flightId),
                seatsCaptor.capture(),
                eq(bookingId),
                eq(HOLD_MINUTES)
        );
        assertEquals(List.of(seatNumber), seatsCaptor.getValue());

        // Verify hold expiration was set before save
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());

        Booking saved = bookingCaptor.getValue();
        assertEquals(bookingId, saved.getId());
        assertEquals(holdExpiresAt, saved.getHoldExpiresAt());
        assertEquals(BookingStatus.DRAFT, saved.getStatus());
    }

    @Test
    void initiateBooking_optimisticLockingFailure_shouldWrapAndRethrowAsIllegalStateException() {
        // Given
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OffsetDateTime holdExpiresAt = OffsetDateTime.now().plusMinutes(15);
        String seatNumber = "1B";

        InitiateBookingSeatCommand command = createValidCommand(flightId, customerId, seatNumber);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("XYZ99999")
                .status(BookingStatus.DRAFT)
                .seats(List.of(BookingSeat.builder().seatNumber(seatNumber).build()))
                .build();

        SeatLockResult lockResult = new SeatLockResult(true, List.of(seatNumber), holdExpiresAt);

        when(bookingCoreService.createDraft(command)).thenReturn(booking);
        when(seatInventoryService.lockSeats(eq(flightId), anyList(), eq(bookingId), eq(HOLD_MINUTES)))
                .thenReturn(lockResult);

        // Simulate concurrent modification
        doThrow(new OptimisticLockingFailureException("Version conflict"))
                .when(bookingRepository).save(any(Booking.class));

        // When & Then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> handler.initiateBooking(command)
        );

        assertEquals("Booking creation failed due to concurrent modification; please retry", ex.getMessage());
        assertInstanceOf(OptimisticLockingFailureException.class, ex.getCause());

        // Verify flow reached the save
        verify(bookingCoreService).createDraft(command);
        verify(seatInventoryService).lockSeats(eq(flightId), anyList(), eq(bookingId), eq(HOLD_MINUTES));
        verify(bookingRepository).save(any(Booking.class));
    }

    // Helper to create a realistic valid command
    private InitiateBookingSeatCommand createValidCommand(UUID flightId, UUID customerId, String seatNumber) {
        var passenger = InitiateBookingSeatCommand.Passenger.builder()
                .firstName("John")
                .lastName("Doe")
                .passengerType("ADULT")
                .email("john.doe@example.com")
                .phone("+911234567890")
                .build();

        var seatSelection = InitiateBookingSeatCommand.SeatSelection.builder()
                .passengerIndex(0)
                .seatNumber(seatNumber)
                .fareClass("ECONOMY")
                .price(BigDecimal.valueOf(8500))
                .build();

        return InitiateBookingSeatCommand.builder()
                .flightId(flightId)
                .customerId(customerId)
                .currency("INR")
                .passengers(List.of(passenger))
                .seatSelections(List.of(seatSelection))
                .build();
    }
}