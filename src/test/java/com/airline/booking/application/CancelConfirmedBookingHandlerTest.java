package com.airline.booking.application;

import com.airline.booking.application.command.CancelConfirmedBookingHandler;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelConfirmedBookingResult;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingCancelationFailedExcpetion;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.shared.events.BookingCancelledEvent;
import com.airline.shared.service.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelConfirmedBookingHandlerTest {

    @Mock
    private IBookingCommandRepository bookingRepository;

    @Mock
    private IBookingService bookingService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CancelConfirmedBookingHandler handler;

    private UUID bookingId;
    private CancelBookingCommand command;
    private UUID flightId;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        flightId = UUID.randomUUID();
        command = new CancelBookingCommand(bookingId, "Test reason");
    }

    //Success
    @Test
    void testCancelConfirmedBooking_Success() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("REF123")
                .flightId(flightId)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        CancelConfirmedBookingResult result = handler.cancelConfirmedBooking(command);

        assertThat(result.getBookingId()).isEqualTo(bookingId);
        assertThat(result.getBookingReference()).isEqualTo("REF123");
        assertThat(result.getStatus()).isEqualTo("CANCELLED");

        verify(bookingService).cancel(booking);

        verify(bookingRepository).save(booking);

        verify(eventPublisher).publish(argThat(event -> 
            event instanceof BookingCancelledEvent && 
            ((BookingCancelledEvent) event).getFlightId().equals(flightId) && 
            ((BookingCancelledEvent) event).getBookingId().equals(bookingId)
        ));
    }

    //AllReady Cancel
    @Test
    void testCancelConfirmedBooking_AlreadyCancelled_Idempotent() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("REF123")
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        CancelConfirmedBookingResult result = handler.cancelConfirmedBooking(command);

        assertThat(result.getBookingId()).isEqualTo(bookingId);
        assertThat(result.getBookingReference()).isEqualTo("REF123");
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(bookingService, never()).cancel(any());
        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    //Error while cancel
    @Test
    void testCancelConfirmedBooking_ExceptionDuringCancel() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("REF123")
                .flightId(flightId)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        doThrow(new RuntimeException("Cancel failure")).when(bookingService).cancel(booking);

        assertThatThrownBy(() -> handler.cancelConfirmedBooking(command))
                .isInstanceOf(BookingCancelationFailedExcpetion.class)
                .hasCauseInstanceOf(RuntimeException.class);

        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}