package com.airline.booking.application;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.FinalizeBookingHandler;
import com.airline.booking.application.command.dto.FinalizeBookingCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.domain.model.Ticket;
import com.airline.booking.domain.model.TicketStatus;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.BookingCoreService;
import com.airline.shared.events.BookingFinalizationFailedEvent;
import com.airline.shared.service.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinalizeBookingHandlerTest {

    @Mock
    private IBookingCommandRepository bookingRepository;

    @Mock
    private BookingCoreService bookingCoreService;

    @Mock
    private SeatInventoryIntegrationService seatInventoryService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private FinalizeBookingHandler handler;

    private UUID bookingId;
    private UUID flightId;
    private UUID customerId;
    private FinalizeBookingCommand command;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        flightId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        command = new FinalizeBookingCommand(bookingId);
    }

    @Test
    void testBookingFinalize_Success_FromPending() {

        UUID seatId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        Booking pendingBooking = Booking.builder()
                .id(bookingId)
                .flightId(flightId)
                .customerId(customerId)
                .bookingReference("REF123")
                .status(BookingStatus.PENDING)
                .seats(List.of(BookingSeat.builder().seatTemplateId(seatId).build()))
                .tickets(List.of())  // Empty initially
                .build();

        Ticket issuedTicket = Ticket.builder()
                .passengerId(passengerId)
                .ticketNumber("TKT123")
                .status(TicketStatus.ISSUED)
                .build();
        Booking confirmedBooking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .tickets(List.of(issuedTicket))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
        doNothing().when(seatInventoryService).bookHoldSeat(any(UUID.class), any(List.class), any(UUID.class), any(UUID.class));
        doNothing().when(bookingCoreService).finalizeBooking(pendingBooking);
        when(bookingRepository.save(pendingBooking)).thenReturn(confirmedBooking);

        ConfirmBookingResult result = handler.bookingFinalize(command);

        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.bookingReference()).isEqualTo("REF123");
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.tickets()).hasSize(1);
        assertThat(result.tickets().get(0).ticketNumber()).isEqualTo("TKT123");
        verify(seatInventoryService).bookHoldSeat(flightId, List.of(seatId), customerId, bookingId);
        verify(bookingCoreService).finalizeBooking(pendingBooking);
        verify(bookingRepository).save(pendingBooking);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testBookingFinalize_Idempotent_AlreadyConfirmed() {
        UUID passengerId = UUID.randomUUID();
        Ticket existingTicket = Ticket.builder()
                .passengerId(passengerId)
                .ticketNumber("TKT123")
                .build();
        Booking confirmedBooking = Booking.builder()
                .id(bookingId)
                .bookingReference("REF123")
                .status(BookingStatus.CONFIRMED)
                .tickets(List.of(existingTicket))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(confirmedBooking));

        // Act
        ConfirmBookingResult result = handler.bookingFinalize(command);

        // Assert
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.bookingReference()).isEqualTo("REF123");
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.tickets()).hasSize(1);
        assertThat(result.tickets().get(0).ticketNumber()).isEqualTo("TKT123");
        verify(seatInventoryService, never()).bookHoldSeat(any(), any(), any(), any());
        verify(bookingCoreService, never()).finalizeBooking(any());
        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testBookingFinalize_BookingNotFound() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.bookingFinalize(command))
                .isInstanceOf(BookingNotFound.class)
                .hasMessageContaining(bookingId.toString());
        verify(seatInventoryService, never()).bookHoldSeat(any(), any(), any(), any());
        verify(bookingCoreService, never()).finalizeBooking(any());
        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testBookingFinalize_ExceptionDuringBookHoldSeat_PublishesEventIfConfirmed() {
        // Arrange
        UUID seatId = UUID.randomUUID();
        Booking pendingBooking = Booking.builder()
                .id(bookingId)
                .flightId(flightId)
                .customerId(customerId)
                .status(BookingStatus.PENDING)
                .seats(List.of(BookingSeat.builder().seatTemplateId(seatId).build()))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
        doThrow(new RuntimeException("Book failed")).when(seatInventoryService).bookHoldSeat(any(), any(), any(), any());

        // Act & Assert
        assertThatThrownBy(() -> handler.bookingFinalize(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Book failed");
        verify(seatInventoryService).bookHoldSeat(flightId, List.of(seatId), customerId, bookingId);
        verify(eventPublisher).publish(argThat(event -> 
            event instanceof BookingFinalizationFailedEvent && 
            ((BookingFinalizationFailedEvent) event).getFlightId().equals(flightId) && 
            ((BookingFinalizationFailedEvent) event).getSeatTemplateIds().contains(seatId) && 
            ((BookingFinalizationFailedEvent) event).getCustomerId().equals(customerId) && 
            ((BookingFinalizationFailedEvent) event).getBookingId().equals(bookingId)
        ));
        verify(bookingCoreService, never()).finalizeBooking(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testBookingFinalize_ExceptionAfterSeatsConfirmed_PublishesEvent() {
        // Arrange
        Booking pendingBooking = Booking.builder()
                .id(bookingId)
                .flightId(flightId)
                .customerId(customerId)
                .status(BookingStatus.PENDING)
                .seats(List.of(BookingSeat.builder().seatTemplateId(UUID.randomUUID()).build()))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
        doNothing().when(seatInventoryService).bookHoldSeat(any(), any(), any(), any());
        doThrow(new RuntimeException("Finalize failed")).when(bookingCoreService).finalizeBooking(pendingBooking);

        // Act & Assert
        assertThatThrownBy(() -> handler.bookingFinalize(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Finalize failed");
        verify(seatInventoryService).bookHoldSeat(any(), any(), any(), any());
        verify(bookingCoreService).finalizeBooking(pendingBooking);
        verify(eventPublisher).publish(any(BookingFinalizationFailedEvent.class));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testBookingFinalize_NoSeatsOrTickets() {
        Booking pendingBooking = Booking.builder()
                .id(bookingId)
                .flightId(flightId)
                .customerId(customerId)
                .status(BookingStatus.PENDING)
                .seats(List.of())
                .build();

        Booking confirmedBooking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .tickets(List.of())
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(pendingBooking));
        doNothing().when(seatInventoryService).bookHoldSeat(any(), any(), any(), any());
        doNothing().when(bookingCoreService).finalizeBooking(pendingBooking);
        when(bookingRepository.save(pendingBooking)).thenReturn(confirmedBooking);

        // Act
        ConfirmBookingResult result = handler.bookingFinalize(command);

        // Assert
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.tickets()).isEmpty();
        verify(seatInventoryService).bookHoldSeat(flightId, List.of(), customerId, bookingId);
    }
}