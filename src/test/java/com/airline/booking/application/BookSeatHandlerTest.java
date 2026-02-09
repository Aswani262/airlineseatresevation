package com.airline.booking.application;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.BookSeatHandler;
import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.domain.model.FareClassCode;
import com.airline.booking.domain.model.Passenger;
import com.airline.booking.domain.model.PassengerType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.airline.flightmgmt.domain.FareClass.ECONOMY;
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
        OffsetDateTime bookingDate = OffsetDateTime.now(ZoneOffset.UTC);

        UUID passengerId1 = UUID.randomUUID();
        UUID passengerId2 = UUID.randomUUID();

        List<Passenger> passengers = List.of(
                Passenger.builder()
                        .id(passengerId1)
                        .bookingId(bookingId)
                        .firstName("A")
                        .lastName("B")
                        .passengerType(PassengerType.ADULT)
                        .email(null)
                        .phone(null)
                        .passportNumber(null)
                        .dateOfBirth(null)
                        .build(),
                Passenger.builder()
                        .id(passengerId2)
                        .bookingId(bookingId)
                        .firstName("C")
                        .lastName("D")
                        .passengerType(PassengerType.CHILD)
                        .email(null)
                        .phone(null)
                        .passportNumber(null)
                        .dateOfBirth(null)
                        .build()
        );

        List<BookingSeat> seats = List.of(
                BookingSeat.builder()
                        .id(UUID.randomUUID())
                        .bookingId(bookingId)
                        .passengerId(passengerId1)
                        .seatNumber("12A")
                        .fareClass(new FareClassCode(ECONOMY.name()))
                        .price(new BigDecimal("100"))
                        .build(),
                BookingSeat.builder()
                        .id(UUID.randomUUID())
                        .bookingId(bookingId)
                        .passengerId(passengerId2)
                        .seatNumber("12B")
                        .fareClass(new FareClassCode(ECONOMY.name()))
                        .price(new BigDecimal("200"))
                        .build()
        );

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .flightId(flightId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("300"))
                .currency("INR")
                .status(BookingStatus.DRAFT)
                .bookingDate(bookingDate)
                .holdExpiresAt(null)
                .passengers(passengers)
                .seats(seats)
                .tickets(new ArrayList<>())
                .build();

        var lockResult = new SeatInventoryService.SeatLockResult(
                true,
                List.of("12A", "12B"),
                holdExpiry
        );

        when(bookingCoreService.createDraft(eq(cmd))).thenReturn(booking);
        when(seatInventoryService.lockSeats(eq(flightId), eq(bookingId), eq(List.of("12A", "12B")), eq(Duration.ofMinutes(10))))
                .thenReturn(lockResult);

        // ensureLockedOrThrow does not throw
        doNothing().when(seatInventoryService).ensureLockedOrThrow(lockResult);

        // When
        BookSeatResult result = handler.book(cmd);

        // Then: return value
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.bookingReference()).isEqualTo(bookingRef);
        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.holdExpiresAt()).isEqualTo(holdExpiry);

        // Then: interactions ordering (roughly)
        verify(bookingCoreService).createDraft(cmd);
        verify(seatInventoryService).lockSeats(flightId, bookingId, List.of("12A", "12B"), Duration.ofMinutes(10));
        verify(seatInventoryService).ensureLockedOrThrow(lockResult);

        // Then: save booking (currency normalized + hold expiry comes from lockResult)
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveBooking(bookingCaptor.capture());

        Booking capturedBooking = bookingCaptor.getValue();
        assertThat(capturedBooking.getCurrency()).isEqualTo("INR");
        assertThat(capturedBooking.getHoldExpiresAt()).isEqualTo(holdExpiry);

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
        OffsetDateTime bookingDate = OffsetDateTime.now(ZoneOffset.UTC);

        UUID passengerId = UUID.randomUUID();

        List<Passenger> passengers = List.of(
                Passenger.builder()
                        .id(passengerId)
                        .bookingId(bookingId)
                        .firstName("A")
                        .lastName("B")
                        .passengerType(PassengerType.ADULT)
                        .email(null)
                        .phone(null)
                        .passportNumber(null)
                        .dateOfBirth(null)
                        .build()
        );

        List<BookingSeat> seats = List.of(
                BookingSeat.builder()
                        .id(UUID.randomUUID())
                        .bookingId(bookingId)
                        .passengerId(passengerId)
                        .seatNumber("12A")
                        .fareClass(new FareClassCode(ECONOMY.name()))
                        .price(BigDecimal.TEN)
                        .build()
        );

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("AB12CD34")
                .flightId(flightId)
                .customerId(customerId)
                .totalAmount(BigDecimal.TEN)
                .currency("INR")
                .status(BookingStatus.DRAFT)
                .bookingDate(bookingDate)
                .holdExpiresAt(null)
                .passengers(passengers)
                .seats(seats)
                .tickets(new ArrayList<>())
                .build();

        var lockResult = new SeatInventoryService.SeatLockResult(
                false,
                List.of("12A"),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10)
        );

        when(bookingCoreService.createDraft(eq(cmd))).thenReturn(booking);
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
        verify(bookingCoreService).createDraft(cmd);
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
        OffsetDateTime bookingDate = OffsetDateTime.now(ZoneOffset.UTC);

        UUID passengerId = UUID.randomUUID();

        List<Passenger> passengers = List.of(
                Passenger.builder()
                        .id(passengerId)
                        .bookingId(bookingId)
                        .firstName("A")
                        .lastName("B")
                        .passengerType(PassengerType.ADULT)
                        .email(null)
                        .phone(null)
                        .passportNumber(null)
                        .dateOfBirth(null)
                        .build()
        );

        List<BookingSeat> seats = List.of(
                BookingSeat.builder()
                        .id(UUID.randomUUID())
                        .bookingId(bookingId)
                        .passengerId(passengerId)
                        .seatNumber("12A")
                        .fareClass(new FareClassCode(ECONOMY.name()))
                        .price(BigDecimal.ONE)
                        .build()
        );

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("AB12CD34")
                .flightId(flightId)
                .customerId(customerId)
                .totalAmount(BigDecimal.ONE)
                .currency("USD")
                .status(BookingStatus.DRAFT)
                .bookingDate(bookingDate)
                .holdExpiresAt(null)
                .passengers(passengers)
                .seats(seats)
                .tickets(new ArrayList<>())
                .build();

        OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        var lockResult = new SeatInventoryService.SeatLockResult(true, List.of("12A"), expiry);

        when(bookingCoreService.createDraft(eq(cmd))).thenReturn(booking);
        when(seatInventoryService.lockSeats(eq(flightId), eq(bookingId), eq(List.of("12A")), eq(Duration.ofMinutes(10))))
                .thenReturn(lockResult);
        doNothing().when(seatInventoryService).ensureLockedOrThrow(lockResult);

        // When
        handler.book(cmd);

        // Then
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveBooking(bookingCaptor.capture());

        assertThat(bookingCaptor.getValue().getCurrency()).isEqualTo("USD");
    }
}