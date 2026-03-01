package com.airline.booking.application;

import com.airline.booking.api.dto.ConfirmedBookingResult;
import com.airline.booking.application.command.ConfirmBookingHandler;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.exception.BookingConfirmationFailedExpection;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.flightmgmt.exception.SeatHoldingExpiredException;
import com.airline.shared.events.BookingConfirmationFailedEvent;
import com.airline.shared.service.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmBookingHandlerTest {

    @Mock
    private IBookingService bookingCoreService;

    @Mock
    private IBookingCommandRepository bookingRepository;

    @Mock
    private SeatInventoryIntegrationService seatInventoryService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ConfirmBookingHandler handler;

    private ConfirmBookingCommand command;
    private UUID bookingId;
    private UUID flightId;
    private UUID customerId;

    @BeforeEach
    void setUp() {

        bookingId = UUID.randomUUID();
        flightId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        command = ConfirmBookingCommand.builder()
                .flightId(flightId)
                .customerId(customerId)
                .passengers(List.of(ConfirmBookingCommand.Passenger.builder().build()))
                .seatSelections(List.of(
                        ConfirmBookingCommand.SeatSelection.builder()
                                .seatTemplateId(UUID.randomUUID())
                                .price(BigDecimal.TEN)
                                .passengerIndex(0)
                                .build()
                ))
                .build();
    }

    @Test
    void testConfirmBooking_Success() {

        Booking pendingBooking = Booking.builder()
                .id(bookingId)
                .bookingReference("REF123")
                .status(BookingStatus.PENDING)
                .seats(List.of(BookingSeat.builder().seatTemplateId(UUID.randomUUID()).build()))
                .build();

        when(bookingCoreService.createPending(command)).thenReturn(pendingBooking);
        doNothing().when(seatInventoryService).extendSeatExpiryTimeForPayment(any(UUID.class), anyList(), any(UUID.class), any(UUID.class));
        when(bookingRepository.save(any(Booking.class))).thenReturn(pendingBooking);

        ConfirmedBookingResult result = handler.confirmBooking(command);

        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.bookingReference()).isEqualTo("REF123");
        assertThat(result.status()).isEqualTo("PENDING");

        verify(bookingCoreService).createPending(command);
        verify(seatInventoryService).extendSeatExpiryTimeForPayment(
                eq(flightId),
                anyList(),
                eq(customerId),
                eq(bookingId)
        );

        verify(bookingRepository).save(pendingBooking);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testConfirmBooking_ExtensionFailure_PublishesEventAndThrows() {
        Booking pendingBooking = Booking.builder()
                .id(bookingId)
                .bookingReference("REF123")
                .status(BookingStatus.PENDING)
                .seats(List.of(BookingSeat.builder().seatTemplateId(UUID.randomUUID()).build()))
                .build();

        when(bookingCoreService.createPending(command)).thenReturn(pendingBooking);

        doThrow(new SeatHoldingExpiredException("Hold expired"))
                .when(seatInventoryService)
                .extendSeatExpiryTimeForPayment(any(UUID.class), anyList(), any(UUID.class), any(UUID.class));

        assertThatThrownBy(() -> handler.confirmBooking(command))
                .isInstanceOf(BookingConfirmationFailedExpection.class)
                .hasCauseInstanceOf(SeatHoldingExpiredException.class);

        verify(bookingCoreService).createPending(command);

        verify(seatInventoryService).extendSeatExpiryTimeForPayment(
                eq(flightId),
                anyList(),
                eq(customerId),
                eq(bookingId)
        );

        verify(bookingRepository, never()).save(any());

        verify(eventPublisher).publish(argThat(event ->
                event instanceof BookingConfirmationFailedEvent &&
                        ((BookingConfirmationFailedEvent) event).getFlightId().equals(flightId) &&
                        ((BookingConfirmationFailedEvent) event).getCustomerId().equals(customerId)
        ));
    }

}