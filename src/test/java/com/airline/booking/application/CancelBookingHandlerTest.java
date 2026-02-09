package com.airline.booking.application;

import com.airline.booking.application.command.CancelBookingHandler;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.airline.booking.repository.BookingQueryRepository;
import com.airline.booking.repository.BookingRepository;
import com.airline.booking.repository.TicketRepository;
import com.airline.booking.service.SeatInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancelBookingHandlerTest {

    private BookingQueryRepository bookingQueryRepository;
    private BookingRepository bookingRepository;
    private SeatInventoryService seatInventoryService;
    private TicketRepository ticketRepository;

    private CancelBookingHandler handler;

    @BeforeEach
    void setup() {
        bookingQueryRepository = mock(BookingQueryRepository.class);
        bookingRepository = mock(BookingRepository.class);
        seatInventoryService = mock(SeatInventoryService.class);
        ticketRepository = mock(TicketRepository.class);

        handler = new CancelBookingHandler(
                bookingQueryRepository,
                bookingRepository,
                seatInventoryService,
                ticketRepository
        );
    }

    @Test
    void cancel_shouldReturnCancelled_whenAlreadyCancelled_idempotent() {
        UUID bookingId = UUID.randomUUID();
        var cmd = CancelBookingCommand.builder().bookingId(bookingId).reason("x").build();

        var snap = snapshot(bookingId, "BR-1", UUID.randomUUID(), "CANCELLED",
                List.of(seatLine(UUID.randomUUID(), "12A", "ECONOMY")),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);

        CancelBookingResult result = handler.cancel(cmd);

        assertThat(result.getBookingId()).isEqualTo(bookingId);
        assertThat(result.getBookingReference()).isEqualTo("BR-1");
        assertThat(result.getStatus()).isEqualTo("CANCELLED");

        verify(bookingQueryRepository).getSnapshot(bookingId);
        verifyNoInteractions(bookingRepository, seatInventoryService, ticketRepository);
    }

    @Test
    void cancel_shouldReturnExpired_whenAlreadyExpired_idempotent() {
        UUID bookingId = UUID.randomUUID();
        var cmd = CancelBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(bookingId, "BR-2", UUID.randomUUID(), "EXPIRED",
                List.of(seatLine(UUID.randomUUID(), "12A", "ECONOMY")),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);

        CancelBookingResult result = handler.cancel(cmd);

        assertThat(result.getStatus()).isEqualTo("EXPIRED");

        verify(bookingQueryRepository).getSnapshot(bookingId);
        verifyNoInteractions(bookingRepository, seatInventoryService, ticketRepository);
    }

    @Test
    void cancel_shouldCancelDraft_releaseLockedSeats_updateStatus_andReturnCancelled() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();

        var cmd = CancelBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(bookingId, "BR-DRAFT", flightId, "DRAFT",
                List.of(
                        seatLine(UUID.randomUUID(), "12A", "ECONOMY"),
                        seatLine(UUID.randomUUID(), "12B", "ECONOMY")
                ),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);
        when(bookingRepository.updateStatus(eq(bookingId), eq("DRAFT"), eq("CANCELLED"), any(OffsetDateTime.class)))
                .thenReturn(1);

        CancelBookingResult result = handler.cancel(cmd);

        assertThat(result.getBookingId()).isEqualTo(bookingId);
        assertThat(result.getBookingReference()).isEqualTo("BR-DRAFT");
        assertThat(result.getStatus()).isEqualTo("CANCELLED");

        // seatNumbers extracted from snapshot (same order)
        verify(seatInventoryService).releaseLockedSeatsOrThrow(
                eq(flightId),
                eq(bookingId),
                eq(List.of("12A", "12B"))
        );

        // ensure updateStatus called with non-null now
        ArgumentCaptor<OffsetDateTime> nowCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(bookingRepository).updateStatus(eq(bookingId), eq("DRAFT"), eq("CANCELLED"), nowCaptor.capture());
        assertThat(nowCaptor.getValue()).isNotNull();

        verifyNoInteractions(ticketRepository);
    }

    @Test
    void cancel_shouldThrow_whenDraftUpdateStatusNotOne_concurrency() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();

        var cmd = CancelBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(bookingId, "BR-DRAFT", flightId, "DRAFT",
                List.of(seatLine(UUID.randomUUID(), "12A", "ECONOMY")),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);
        when(bookingRepository.updateStatus(eq(bookingId), eq("DRAFT"), eq("CANCELLED"), any(OffsetDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> handler.cancel(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("concurrently");

        verify(seatInventoryService).releaseLockedSeatsOrThrow(flightId, bookingId, List.of("12A"));
        verify(bookingRepository).updateStatus(eq(bookingId), eq("DRAFT"), eq("CANCELLED"), any(OffsetDateTime.class));
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void cancel_shouldCancelConfirmed_cancelTickets_releaseBookedSeats_updateStatus_andReturnCancelled_inOrder() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();

        var cmd = CancelBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(bookingId, "BR-CONF", flightId, "CONFIRMED",
                List.of(
                        seatLine(UUID.randomUUID(), "1A", "BUSINESS"),
                        seatLine(UUID.randomUUID(), "1B", "BUSINESS")
                ),
                List.of(
                        passengerLine(UUID.randomUUID(), "A", "B"),
                        passengerLine(UUID.randomUUID(), "C", "D")
                )
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);
        when(bookingRepository.updateStatus(eq(bookingId), eq("CONFIRMED"), eq("CANCELLED"), any(OffsetDateTime.class)))
                .thenReturn(1);

        CancelBookingResult result = handler.cancel(cmd);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");

        InOrder inOrder = inOrder(ticketRepository, seatInventoryService, bookingRepository);

        inOrder.verify(ticketRepository).cancelTicketsByBooking(bookingId);
        inOrder.verify(seatInventoryService).releaseBookedSeats(flightId, List.of("1A", "1B"));
        inOrder.verify(bookingRepository).updateStatus(eq(bookingId), eq("CONFIRMED"), eq("CANCELLED"), any(OffsetDateTime.class));
    }

    @Test
    void cancel_shouldThrow_whenConfirmedUpdateStatusNotOne_concurrency() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();

        var cmd = CancelBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(bookingId, "BR-CONF", flightId, "CONFIRMED",
                List.of(seatLine(UUID.randomUUID(), "1A", "BUSINESS")),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);
        when(bookingRepository.updateStatus(eq(bookingId), eq("CONFIRMED"), eq("CANCELLED"), any(OffsetDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> handler.cancel(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("concurrently");

        verify(ticketRepository).cancelTicketsByBooking(bookingId);
        verify(seatInventoryService).releaseBookedSeats(flightId, List.of("1A"));
        verify(bookingRepository).updateStatus(eq(bookingId), eq("CONFIRMED"), eq("CANCELLED"), any(OffsetDateTime.class));
    }

    @Test
    void cancel_shouldThrow_forUnsupportedStatus() {
        UUID bookingId = UUID.randomUUID();
        var cmd = CancelBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(bookingId, "BR-X", UUID.randomUUID(), "PENDING_PAYMENT",
                List.of(seatLine(UUID.randomUUID(), "12A", "ECONOMY")),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);

        assertThatThrownBy(() -> handler.cancel(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be cancelled")
                .hasMessageContaining("PENDING_PAYMENT");

        verify(bookingQueryRepository).getSnapshot(bookingId);
        verifyNoInteractions(bookingRepository, seatInventoryService, ticketRepository);
    }

    // ---------------- helpers ----------------

    private static BookingQueryRepository.BookingSnapshot snapshot(
            UUID bookingId,
            String bookingRef,
            UUID flightId,
            String status,
            List<BookingQueryRepository.BookingSnapshot.SeatLine> seats,
            List<BookingQueryRepository.BookingSnapshot.PassengerLine> passengers
    ) {
        return new BookingQueryRepository.BookingSnapshot(
                bookingId,
                bookingRef,
                flightId,
                status,
                OffsetDateTime.now(), // not used by cancel logic
                seats,
                passengers
        );
    }

    private static BookingQueryRepository.BookingSnapshot.SeatLine seatLine(UUID passengerId, String seat, String fareClass) {
        return new BookingQueryRepository.BookingSnapshot.SeatLine(passengerId, seat, fareClass);
    }

    private static BookingQueryRepository.BookingSnapshot.PassengerLine passengerLine(UUID passengerId, String fn, String ln) {
        return new BookingQueryRepository.BookingSnapshot.PassengerLine(passengerId, fn, ln);
    }
}
