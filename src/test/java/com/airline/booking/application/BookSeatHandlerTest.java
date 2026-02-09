package com.airline.booking.application;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.BookSeatHandler;
import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.repository.BookingRepository;
import com.airline.booking.service.BookingCoreService;
import com.airline.booking.service.SeatInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookSeatHandlerTest {

    private BookingCoreService bookingCoreService;
    private SeatInventoryService seatInventoryService;
    private BookingRepository bookingRepository;

    private BookSeatHandler handler;

    @BeforeEach
    void setup() {
        bookingCoreService = mock(BookingCoreService.class);
        seatInventoryService = mock(SeatInventoryService.class);
        bookingRepository = mock(BookingRepository.class);

        handler = new BookSeatHandler(bookingCoreService, seatInventoryService, bookingRepository);
    }

    @Test
    void book_shouldLockSeatsPersistBookingPassengersSeats_andReturnResult() {
        // Given
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(flightId)
                .customerId(customerId)
                .currency("  inr ")
                .passengers(List.of(
                        BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build(),
                        BookSeatCommand.Passenger.builder().firstName("C").lastName("D").passengerType("CHILD").build()
                ))
                .seatSelections(List.of(
                        BookSeatCommand.SeatSelection.builder()
                                .passengerIndex(0).seatNumber("12A").fareClass("ECONOMY").price(new BigDecimal("100")).build(),
                        BookSeatCommand.SeatSelection.builder()
                                .passengerIndex(1).seatNumber("12B").fareClass("ECONOMY").price(new BigDecimal("200")).build()
                ))
                .build();

        UUID bookingId = UUID.randomUUID();
        String bookingRef = "AB12CD34";
        OffsetDateTime holdExpiry = OffsetDateTime.of(2026, 2, 8, 12, 0, 0, 0, ZoneOffset.UTC);

        var draft = new BookingCoreService.BookingDraft(
                bookingId,
                bookingRef,
                "DRAFT",
                holdExpiry.minusMinutes(5), // draft's holdExpiresAt is not used by handler
                new BigDecimal("300"),
                List.of("12A", "12B")
        );

        var lockResult = new SeatInventoryService.SeatLockResult(
                true,
                List.of("12A", "12B"),
                holdExpiry
        );

        when(bookingCoreService.createDraft(eq(cmd), eq(10))).thenReturn(draft);
        when(seatInventoryService.lockSeats(eq(flightId), eq(bookingId), eq(List.of("12A", "12B")), eq(Duration.ofMinutes(10))))
                .thenReturn(lockResult);

        // ensureLockedOrThrow does not throw
        doNothing().when(seatInventoryService).ensureLockedOrThrow(lockResult);

        List<UUID> passengerIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(bookingRepository.insertPassengers(eq(bookingId), eq(cmd.getPassengers()))).thenReturn(passengerIds);

        // When
        BookSeatResult result = handler.book(cmd);

        // Then: return value
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.bookingReference()).isEqualTo(bookingRef);
        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.holdExpiresAt()).isEqualTo(holdExpiry);

        // Then: interactions ordering (roughly)
        verify(bookingCoreService).createDraft(cmd, 10);
        verify(seatInventoryService).lockSeats(flightId, bookingId, List.of("12A", "12B"), Duration.ofMinutes(10));
        verify(seatInventoryService).ensureLockedOrThrow(lockResult);

        // Then: booking insert args (currency normalized + hold expiry comes from lockResult)
        verify(bookingRepository).insertBooking(
                eq(bookingId),
                eq(bookingRef),
                eq(flightId),
                eq(customerId),
                eq(new BigDecimal("300")),
                eq("INR"),
                eq("DRAFT"),
                eq(holdExpiry)
        );

        verify(bookingRepository).insertPassengers(bookingId, cmd.getPassengers());
        verify(bookingRepository).insertBookingSeats(bookingId, cmd.getSeatSelections(), passengerIds);

        verifyNoMoreInteractions(bookingCoreService, seatInventoryService, bookingRepository);
    }

    @Test
    void book_shouldNotPersistAnything_whenSeatLockFails() {
        // Given
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(flightId)
                .customerId(customerId)
                .currency("INR")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder()
                        .passengerIndex(0).seatNumber("12A").fareClass("ECONOMY").price(BigDecimal.TEN).build()))
                .build();

        UUID bookingId = UUID.randomUUID();

        var draft = new BookingCoreService.BookingDraft(
                bookingId,
                "AB12CD34",
                "DRAFT",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),
                BigDecimal.TEN,
                List.of("12A")
        );

        var lockResult = new SeatInventoryService.SeatLockResult(
                false,
                List.of("12A"),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10)
        );

        when(bookingCoreService.createDraft(eq(cmd), eq(10))).thenReturn(draft);
        when(seatInventoryService.lockSeats(eq(flightId), eq(bookingId), eq(List.of("12A")), eq(Duration.ofMinutes(10))))
                .thenReturn(lockResult);

        // ensureLockedOrThrow throws
        doThrow(new IllegalStateException("One or more seats are not available"))
                .when(seatInventoryService).ensureLockedOrThrow(lockResult);

        // When / Then
        assertThatThrownBy(() -> handler.book(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");

        // bookingRepository should never be called
        verifyNoInteractions(bookingRepository);

        // core service + seat service should be called up to the point of failure
        verify(bookingCoreService).createDraft(cmd, 10);
        verify(seatInventoryService).lockSeats(flightId, bookingId, List.of("12A"), Duration.ofMinutes(10));
        verify(seatInventoryService).ensureLockedOrThrow(lockResult);

        verifyNoMoreInteractions(bookingCoreService, seatInventoryService);
    }

    @Test
    void book_shouldUppercaseTrimCurrency_beforeInsertBooking() {
        // Given a weird currency input
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(flightId)
                .customerId(customerId)
                .currency("  usd ")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder()
                        .passengerIndex(0).seatNumber("12A").fareClass("ECONOMY").price(BigDecimal.ONE).build()))
                .build();

        UUID bookingId = UUID.randomUUID();
        var draft = new BookingCoreService.BookingDraft(
                bookingId, "AB12CD34", "DRAFT",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),
                BigDecimal.ONE,
                List.of("12A")
        );

        OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        var lockResult = new SeatInventoryService.SeatLockResult(true, List.of("12A"), expiry);

        when(bookingCoreService.createDraft(eq(cmd), eq(10))).thenReturn(draft);
        when(seatInventoryService.lockSeats(eq(flightId), eq(bookingId), eq(List.of("12A")), eq(Duration.ofMinutes(10))))
                .thenReturn(lockResult);
        doNothing().when(seatInventoryService).ensureLockedOrThrow(lockResult);

        when(bookingRepository.insertPassengers(eq(bookingId), anyList()))
                .thenReturn(List.of(UUID.randomUUID()));

        // capture currency argument
        ArgumentCaptor<String> currencyCaptor = ArgumentCaptor.forClass(String.class);

        // When
        handler.book(cmd);

        // Then
        verify(bookingRepository).insertBooking(
                eq(bookingId),
                eq(draft.bookingReference()),
                eq(flightId),
                eq(customerId),
                eq(draft.totalAmount()),
                currencyCaptor.capture(),
                eq(draft.status()),
                eq(expiry)
        );

        assertThat(currencyCaptor.getValue()).isEqualTo("USD");
    }
}
