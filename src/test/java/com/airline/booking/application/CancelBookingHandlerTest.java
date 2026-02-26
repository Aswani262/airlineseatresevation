package com.airline.booking.application;

import com.airline.booking.application.command.CancelBookingHandler;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingFailed;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.flightmgmt.exception.SeatHoldingFailedException;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.flightmgmt.service.ISeatInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelBookingHandlerTest {

    @InjectMocks
    private CancelBookingHandler cancelBookingHandler;

    @Mock
    private IBookingCommandRepository bookingRepository;

    @Mock
    private ISeatInventoryService seatInventoryService;

    @Mock
    private ISeatInventoryCommandRepository seatInventoryRepository;

    @Mock
    private IBookingService bookingService;

    private CancelBookingCommand command;
    private Booking mockBooking;
    private List<SeatAssignments> mockSeats;

    @BeforeEach
    void setUp() {
        UUID bookingId = UUID.randomUUID();
        command = CancelBookingCommand.builder()
                .bookingId(bookingId)
                .reason("Test reason")
                .build();

        mockBooking = Booking.builder()
                .id(bookingId)
                .bookingReference("REF123")
                .flightId(UUID.randomUUID())
                .status(BookingStatus.DRAFT)
                .seats(List.of(
                        BookingSeat.builder().seatNumber("A1").build(),
                        BookingSeat.builder().seatNumber("A2").build()
                ))
                .build();

        mockSeats = List.of(
                SeatAssignments.builder()
                        .id(UUID.randomUUID())
                        .flightId(mockBooking.getFlightId())
                        .seatNumber("A1")
                        .build(),
                SeatAssignments.builder()
                        .id(UUID.randomUUID())
                        .flightId(mockBooking.getFlightId())
                        .seatNumber("A2")
                        .build()
        );
    }

    @Test
    void cancel_fromDraft_releasesLocksAndCancels_returnsCancelled() {
        // Arrange
        mockBooking.setStatus(BookingStatus.DRAFT);
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndTemplateIdIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(mockSeats);
        doNothing().when(seatInventoryService).releaseLockedSeatsOrThrow(mockSeats, mockBooking.getId());
        when(seatInventoryRepository.saveAll(anyIterable())).thenReturn(mockSeats);
        doNothing().when(bookingService).cancel(mockBooking);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        // Act
        CancelBookingResult result = cancelBookingHandler.cancel(command);

        // Assert
        assertNotNull(result);
        assertEquals(mockBooking.getId(), result.getBookingId());
        assertEquals(mockBooking.getBookingReference(), result.getBookingReference());
        assertEquals("CANCELLED", result.getStatus());

        // Verify interactions
        verify(seatInventoryService).releaseLockedSeatsOrThrow(mockSeats, mockBooking.getId());
        verify(seatInventoryService, never()).releaseBookedSeats(any());
        verify(seatInventoryRepository).saveAll(mockSeats);
        verify(bookingService).cancel(mockBooking);
        verify(bookingRepository).save(mockBooking);
    }

    @Test
    void cancel_fromConfirmed_releasesBookedAndCancels_returnsCancelled() {
        // Arrange
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndTemplateIdIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(mockSeats);
        doNothing().when(seatInventoryService).releaseBookedSeats(mockSeats);
        when(seatInventoryRepository.saveAll(anyIterable())).thenReturn(mockSeats);
        doNothing().when(bookingService).cancel(mockBooking);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        // Act
        CancelBookingResult result = cancelBookingHandler.cancel(command);

        // Assert
        assertNotNull(result);
        assertEquals(mockBooking.getId(), result.getBookingId());
        assertEquals(mockBooking.getBookingReference(), result.getBookingReference());
        assertEquals("CANCELLED", result.getStatus());

        // Verify interactions
        verify(seatInventoryService, never()).releaseLockedSeatsOrThrow(any(), any());
        verify(seatInventoryService).releaseBookedSeats(mockSeats);
        verify(seatInventoryRepository).saveAll(mockSeats);
        verify(bookingService).cancel(mockBooking);
        verify(bookingRepository).save(mockBooking);
    }

    @Test
    void cancel_alreadyCancelled_returnsCancelled() {
        // Arrange
        mockBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));

        // Act
        CancelBookingResult result = cancelBookingHandler.cancel(command);

        // Assert
        assertEquals("CANCELLED", result.getStatus());

        // Verify no further interactions
        verify(seatInventoryService, never()).normalizeSeats(any());
        verify(seatInventoryRepository, never()).findByFlightIdAndTemplateIdIn(any(), any());
        verify(seatInventoryService, never()).releaseLockedSeatsOrThrow(any(), any());
        verify(seatInventoryService, never()).releaseBookedSeats(any());
        verify(seatInventoryRepository, never()).saveAll(any());
        verify(bookingService, never()).cancel(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancel_alreadyExpired_returnsExpired() {
        // Arrange
        mockBooking.setStatus(BookingStatus.EXPIRED);
        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));

        // Act
        CancelBookingResult result = cancelBookingHandler.cancel(command);

        // Assert
        assertEquals("EXPIRED", result.getStatus());

        // Verify no further interactions
        verify(seatInventoryService, never()).normalizeSeats(any());
        verify(seatInventoryRepository, never()).findByFlightIdAndTemplateIdIn(any(), any());
        verify(seatInventoryService, never()).releaseLockedSeatsOrThrow(any(), any());
        verify(seatInventoryService, never()).releaseBookedSeats(any());
        verify(seatInventoryRepository, never()).saveAll(any());
        verify(bookingService, never()).cancel(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancel_bookingNotFound_throwsIllegalStateException() {
        // Arrange
        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> cancelBookingHandler.cancel(command));
        assertEquals("Booking not found: " + command.getBookingId(), exception.getMessage());

        // Verify no further interactions
        verify(seatInventoryService, never()).normalizeSeats(any());
    }



    @Test
    void cancel_seatsNotFound_throwsIllegalStateException() {
        // Arrange
        mockBooking.setStatus(BookingStatus.DRAFT);
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");
        List<SeatAssignments> partialSeats = List.of(mockSeats.get(0)); // Mismatch

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndTemplateIdIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(partialSeats);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> cancelBookingHandler.cancel(command));
        assertEquals("One or more seats not found", exception.getMessage());

        // Verify no further
        verify(seatInventoryService, never()).releaseLockedSeatsOrThrow(any(), any());
        verify(seatInventoryRepository, never()).saveAll(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancel_optimisticLockingFailureOnSeats_throwsIllegalStateException() {
        // Arrange
        mockBooking.setStatus(BookingStatus.DRAFT);
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndTemplateIdIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(mockSeats);
        doNothing().when(seatInventoryService).releaseLockedSeatsOrThrow(mockSeats, mockBooking.getId());
        when(seatInventoryRepository.saveAll(anyIterable())).thenThrow(OptimisticLockingFailureException.class);

        // Act & Assert
        SeatHoldingFailedException exception = assertThrows(SeatHoldingFailedException.class, () -> cancelBookingHandler.cancel(command));
        assertEquals("Seat release failed due to concurrent modification; please retry", exception.getMessage());

        // Verify no booking save
        verify(bookingService, never()).cancel(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancel_optimisticLockingFailureOnBooking_throwsIllegalStateException() {
        // Arrange
        mockBooking.setStatus(BookingStatus.DRAFT);
        List<String> seatNumbers = List.of("A1", "A2");
        List<String> normalizedSeats = List.of("A1", "A2");

        when(bookingRepository.findById(command.getBookingId())).thenReturn(Optional.of(mockBooking));
        when(seatInventoryService.normalizeSeats(seatNumbers)).thenReturn(normalizedSeats);
        when(seatInventoryRepository.findByFlightIdAndTemplateIdIn(mockBooking.getFlightId(), normalizedSeats)).thenReturn(mockSeats);
        doNothing().when(seatInventoryService).releaseLockedSeatsOrThrow(mockSeats, mockBooking.getId());
        when(seatInventoryRepository.saveAll(anyIterable())).thenReturn(mockSeats);
        doNothing().when(bookingService).cancel(mockBooking);
        when(bookingRepository.save(any(Booking.class))).thenThrow(OptimisticLockingFailureException.class);

        // Act & Assert
        assertThrows(BookingFailed.class, () -> cancelBookingHandler.cancel(command));
    }
}