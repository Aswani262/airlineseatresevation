package com.airline.booking.application;

import com.airline.booking.application.command.CancelPendingBookingHandler;
import com.airline.booking.application.command.dto.CancelPendingBookingCommand;
import com.airline.booking.application.command.dto.CancelPendingBookingResult;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingCancelationFailedExcpetion;
import com.airline.booking.exception.BookingNotFound;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.IBookingService;
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
class CancelPendingBookingHandlerTest {

    @Mock
    private IBookingCommandRepository bookingRepository;

    @Mock
    private IBookingService bookingService;

    @InjectMocks
    private CancelPendingBookingHandler handler;

    private UUID bookingId;
    private CancelPendingBookingCommand command;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        command = new CancelPendingBookingCommand(bookingId, "Test reason");
    }

    @Test
    void testCancelPendingBooking_Success() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        CancelPendingBookingResult result = handler.cancelPendingBooking(command);

        assertThat(result.getBookingId()).isEqualTo(bookingId);
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(bookingService).cancel(booking);
        verify(bookingRepository).save(booking);
    }


    @Test
    void testCancelPendingBooking_AlreadyCancelled_Idempotent() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act
        CancelPendingBookingResult result = handler.cancelPendingBooking(command);

        // Assert
        assertThat(result.getBookingId()).isEqualTo(bookingId);
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(bookingService, never()).cancel(any());
        verify(bookingRepository, never()).save(any());
    }



    @Test
    void testCancelPendingBooking_ExceptionDuringCancel() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        doThrow(new RuntimeException("Cancel failure")).when(bookingService).cancel(booking);

        assertThatThrownBy(() -> handler.cancelPendingBooking(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cancel failure");
        verify(bookingRepository, never()).save(any());
    }
}