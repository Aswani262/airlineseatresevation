package com.airline.booking.application;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.ConfirmBookingHandler;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.domain.model.Passenger;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.exception.SeatNotFound;
import com.airline.flightmgmt.domain.SeatInventory;
import com.airline.booking.domain.model.Ticket;
import com.airline.booking.domain.model.TicketStatus;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.booking.service.core.BookingCoreService;
import com.airline.flightmgmt.service.ISeatInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmBookingHandlerTest {

    @InjectMocks
    private ConfirmBookingHandler confirmBookingHandler;

    @Mock
    private IBookingCommandRepository bookingRepository;

    @Mock
    private ISeatInventoryService seatInventoryService;

    @Mock
    private ISeatInventoryCommandRepository seatInventoryRepository;

    @Mock
    private BookingCoreService bookingCoreService;

    private ConfirmBookingCommand command;
    private Booking mockBooking;
    private List<SeatInventory> mockSeats;

    @BeforeEach
    void setUp() {
        UUID bookingId = UUID.randomUUID();
        command = ConfirmBookingCommand.builder()
                .bookingId(bookingId)
                .paymentId(UUID.randomUUID())
                .transactionId("TX123")
                .build();

        UUID passengerId1 = UUID.randomUUID();
        UUID passengerId2 = UUID.randomUUID();

        mockBooking = Booking.builder()
                .id(bookingId)
                .bookingReference("REF123")
                .flightId(UUID.randomUUID())
                .status(BookingStatus.DRAFT)
                .holdExpiresAt(OffsetDateTime.now().plusMinutes(5))
                .passengers(List.of(
                        Passenger.builder().id(passengerId1).build(),
                        Passenger.builder().id(passengerId2).build()
                ))
                .seats(List.of(
                        BookingSeat.builder().seatNumber("A1").build(),
                        BookingSeat.builder().seatNumber("A2").build()
                ))
                .tickets(List.of())
                .build();

        mockSeats = List.of(
                SeatInventory.builder()
                        .id(UUID.randomUUID())
                        .flightId(mockBooking.getFlightId())
                        .seatNumber("A1")
                        .build(),
                SeatInventory.builder()
                        .id(UUID.randomUUID())
                        .flightId(mockBooking.getFlightId())
                        .seatNumber("A2")
                        .build()
        );
    }

    @Test
    void bookingFinalize_fromDraft_confirmsAndReturnsResult() {
        // Arrange
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");

        // Simulate confirm changing status and adding tickets
        doAnswer(invocation -> {
            mockBooking.setStatus(BookingStatus.CONFIRMED);
            mockBooking.setTickets(List.of(
                    Ticket.builder().passengerId(mockBooking.getPassengers().get(0).getId()).ticketNumber("TKT1").status(TicketStatus.ISSUED).build(),
                    Ticket.builder().passengerId(mockBooking.getPassengers().get(1).getId()).ticketNumber("TKT2").status(TicketStatus.ISSUED).build()
            ));
            return null;
        }).when(bookingCoreService).confirm(mockBooking);

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndSeatNumberIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(mockSeats);
        doNothing().when(seatInventoryService).confirmLockedSeatsOrThrow(mockSeats, mockBooking.getId());
        when(seatInventoryRepository.saveAll(anyIterable())).thenReturn(mockSeats);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        // Act
        ConfirmBookingResult result = confirmBookingHandler.bookingFinalize(command);

        // Assert
        assertNotNull(result);
        assertEquals(mockBooking.getId(), result.bookingId());
        assertEquals(mockBooking.getBookingReference(), result.bookingReference());
        assertEquals("CONFIRMED", result.status());
        assertEquals(2, result.tickets().size());
        assertEquals("TKT1", result.tickets().get(0).ticketNumber());

        // Verify interactions
        verify(bookingCoreService).confirm(mockBooking);
        verify(seatInventoryService).normalizeSeats(seatNumbers);
        verify(seatInventoryRepository).findByFlightIdAndSeatNumberIn(mockBooking.getFlightId(), normalizedSeats);
        verify(seatInventoryService).confirmLockedSeatsOrThrow(mockSeats, mockBooking.getId());
        verify(seatInventoryRepository).saveAll(mockSeats);
        verify(bookingRepository).save(mockBooking);
    }

    @Test
    void bookingFinalize_alreadyConfirmed_returnsIdempotentResult() {
        // Arrange
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        mockBooking.setTickets(List.of(
                Ticket.builder().passengerId(mockBooking.getPassengers().get(0).getId()).ticketNumber("TKT1").status(TicketStatus.ISSUED).build(),
                Ticket.builder().passengerId(mockBooking.getPassengers().get(1).getId()).ticketNumber("TKT2").status(TicketStatus.ISSUED).build()
        ));

        // Simulate confirm doing nothing (idempotent)
        doNothing().when(bookingCoreService).confirm(mockBooking);

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));

        // Act
        ConfirmBookingResult result = confirmBookingHandler.bookingFinalize(command);

        // Assert
        assertEquals("CONFIRMED", result.status());
        assertEquals(2, result.tickets().size());
        assertEquals("TKT1", result.tickets().get(0).ticketNumber());

        // Verify no further actions
        verify(bookingCoreService).confirm(mockBooking);
        verify(seatInventoryService, never()).normalizeSeats(any());
        verify(seatInventoryRepository, never()).findByFlightIdAndSeatNumberIn(any(), any());
        verify(seatInventoryService, never()).confirmLockedSeatsOrThrow(any(), any());
        verify(seatInventoryRepository, never()).saveAll(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookingFinalize_bookingNotFound_throwsIllegalStateException() {
        // Arrange
        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.empty());

        // Act & Assert
         assertThrows(BookingNotFound.class, () -> confirmBookingHandler.bookingFinalize(command));

        verify(bookingCoreService, never()).confirm(any());
    }

    @Test
    void bookingFinalize_seatsNotFound_throwsIllegalStateException() {
        // Arrange
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");
        List<SeatInventory> partialSeats = List.of(mockSeats.get(0)); // Mismatch

        doAnswer(invocation -> {
            mockBooking.setStatus(BookingStatus.CONFIRMED);
            return null;
        }).when(bookingCoreService).confirm(mockBooking);

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndSeatNumberIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(partialSeats);

        // Act & Assert
        assertThrows(SeatNotFound.class, () -> confirmBookingHandler.bookingFinalize(command));

        verify(seatInventoryService, never()).confirmLockedSeatsOrThrow(any(), any());
        verify(seatInventoryRepository, never()).saveAll(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookingFinalize_confirmSeatsThrows_throwsException() {
        // Arrange
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");

        doAnswer(invocation -> {
            mockBooking.setStatus(BookingStatus.CONFIRMED);
            return null;
        }).when(bookingCoreService).confirm(mockBooking);

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndSeatNumberIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(mockSeats);
        doThrow(new IllegalStateException("Seats not locked")).when(seatInventoryService).confirmLockedSeatsOrThrow(mockSeats, mockBooking.getId());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> confirmBookingHandler.bookingFinalize(command));
        assertEquals("Seats not locked", exception.getMessage());

        verify(seatInventoryRepository, never()).saveAll(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookingFinalize_optimisticLockingFailureOnSeats_throwsIllegalStateException() {
        // Arrange
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");

        doAnswer(invocation -> {
            mockBooking.setStatus(BookingStatus.CONFIRMED);
            return null;
        }).when(bookingCoreService).confirm(mockBooking);

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndSeatNumberIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(mockSeats);
        doNothing().when(seatInventoryService).confirmLockedSeatsOrThrow(mockSeats, mockBooking.getId());
        when(seatInventoryRepository.saveAll(anyIterable())).thenThrow(OptimisticLockingFailureException.class);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> confirmBookingHandler.bookingFinalize(command));
        assertEquals("Seat confirmation failed due to concurrent modification; please retry", exception.getMessage());

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookingFinalize_optimisticLockingFailureOnBooking_throwsIllegalStateException() {
        // Arrange
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");

        doAnswer(invocation -> {
            mockBooking.setStatus(BookingStatus.CONFIRMED);
            return null;
        }).when(bookingCoreService).confirm(mockBooking);

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndSeatNumberIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(mockSeats);
        doNothing().when(seatInventoryService).confirmLockedSeatsOrThrow(mockSeats, mockBooking.getId());
        when(seatInventoryRepository.saveAll(anyIterable())).thenReturn(mockSeats);
        when(bookingRepository.save(any(Booking.class))).thenThrow(OptimisticLockingFailureException.class);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> confirmBookingHandler.bookingFinalize(command));
        assertEquals("Booking status changed concurrently; please retry", exception.getMessage());
    }
}