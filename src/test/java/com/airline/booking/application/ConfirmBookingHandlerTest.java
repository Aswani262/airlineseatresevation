package com.airline.booking.application;

import com.airline.booking.api.dto.ConfirmBookingResult;
import com.airline.booking.application.command.ConfirmBookingHandler;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.booking.repository.BookingQueryRepository;
import com.airline.booking.repository.BookingRepository;
import com.airline.booking.repository.TicketRepository;
import com.airline.booking.service.SeatInventoryService;
import com.airline.booking.service.TicketingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfirmBookingHandlerTest {

    private BookingQueryRepository bookingQueryRepository;
    private BookingRepository bookingRepository;
    private SeatInventoryService seatInventoryService;
    private TicketRepository ticketRepository;
    private TicketingService ticketingService;

    private ConfirmBookingHandler handler;

    @BeforeEach
    void setup() {
        bookingQueryRepository = mock(BookingQueryRepository.class);
        bookingRepository = mock(BookingRepository.class);
        seatInventoryService = mock(SeatInventoryService.class);
        ticketRepository = mock(TicketRepository.class);
        ticketingService = mock(TicketingService.class);

        handler = new ConfirmBookingHandler(
                bookingQueryRepository,
                bookingRepository,
                seatInventoryService,
                ticketRepository,
                ticketingService
        );
    }

    @Test
    void bookingFinalize_shouldReturnIdempotentConfirmed_whenAlreadyConfirmed() {
        UUID bookingId = UUID.randomUUID();
        var cmd = ConfirmBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(
                bookingId, "BR-OK", UUID.randomUUID(),
                "CONFIRMED",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),
                List.of(seatLine(UUID.randomUUID(), "12A", "ECONOMY")),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);

        ConfirmBookingResult result = handler.bookingFinalize(cmd);

        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.bookingReference()).isEqualTo("BR-OK");
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.tickets()).isEmpty();

        verify(bookingQueryRepository).getSnapshot(bookingId);
        verifyNoInteractions(bookingRepository, seatInventoryService, ticketRepository, ticketingService);
    }

    @Test
    void bookingFinalize_shouldThrow_whenStatusIsNotDraft() {
        UUID bookingId = UUID.randomUUID();
        var cmd = ConfirmBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(
                bookingId, "BR-X", UUID.randomUUID(),
                "CANCELLED",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),
                List.of(seatLine(UUID.randomUUID(), "12A", "ECONOMY")),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);

        assertThatThrownBy(() -> handler.bookingFinalize(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be confirmed")
                .hasMessageContaining("CANCELLED");

        verify(bookingQueryRepository).getSnapshot(bookingId);
        verifyNoInteractions(bookingRepository, seatInventoryService, ticketRepository, ticketingService);
    }

    @Test
    void bookingFinalize_shouldThrow_whenHoldExpired() {
        UUID bookingId = UUID.randomUUID();
        var cmd = ConfirmBookingCommand.builder().bookingId(bookingId).build();

        var snap = snapshot(
                bookingId, "BR-EXP", UUID.randomUUID(),
                "DRAFT",
                OffsetDateTime.now(Clock.systemUTC()).minusSeconds(1), // already expired
                List.of(seatLine(UUID.randomUUID(), "12A", "ECONOMY")),
                List.of(passengerLine(UUID.randomUUID(), "A", "B"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);

        assertThatThrownBy(() -> handler.bookingFinalize(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hold expired");

        verify(bookingQueryRepository).getSnapshot(bookingId);
        verifyNoInteractions(bookingRepository, seatInventoryService, ticketRepository, ticketingService);
    }

    @Test
    void bookingFinalize_shouldConfirmSeats_updateStatus_issueTickets_andReturnResponse() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        var cmd = ConfirmBookingCommand.builder().bookingId(bookingId).build();

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        var snap = snapshot(
                bookingId, "BR-DRAFT", flightId,
                "DRAFT",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),
                List.of(
                        seatLine(p1, "12A", "ECONOMY"),
                        seatLine(p2, "12B", "ECONOMY")
                ),
                List.of(
                        passengerLine(p1, "John", "Doe"),
                        passengerLine(p2, "Jane", "Roe")
                )
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);

        // seat confirmation doesn't throw
        doNothing().when(seatInventoryService)
                .confirmLockedSeatsOrThrow(flightId, bookingId, List.of("12A", "12B"));

        when(bookingRepository.updateStatus(eq(bookingId), eq("DRAFT"), eq("CONFIRMED"), any(OffsetDateTime.class)))
                .thenReturn(1);

        when(ticketingService.generateTicketNumber())
                .thenReturn("1760000000001", "1760000000002");

        // capture tickets inserted
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TicketRepository.TicketRow>> ticketCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(ticketRepository).insertTickets(ticketCaptor.capture());

        ConfirmBookingResult result = handler.bookingFinalize(cmd);

        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.bookingReference()).isEqualTo("BR-DRAFT");
        assertThat(result.status()).isEqualTo("CONFIRMED");

        assertThat(result.tickets())
                .extracting(ConfirmBookingResult.TicketIssued::passengerId,
                        ConfirmBookingResult.TicketIssued::ticketNumber)
                .containsExactlyInAnyOrder(
                        tuple(p1, "1760000000001"),
                        tuple(p2, "1760000000002")
                );

        // verify side-effect order (seat confirm -> status update -> ticket insert)
        InOrder inOrder = inOrder(seatInventoryService, bookingRepository, ticketRepository);

        inOrder.verify(seatInventoryService).confirmLockedSeatsOrThrow(flightId, bookingId, List.of("12A", "12B"));
        inOrder.verify(bookingRepository).updateStatus(eq(bookingId), eq("DRAFT"), eq("CONFIRMED"), any(OffsetDateTime.class));
        inOrder.verify(ticketRepository).insertTickets(anyList());

        // verify generated tickets were inserted as expected
        List<TicketRepository.TicketRow> inserted = ticketCaptor.getValue();
        assertThat(inserted).hasSize(2);

        // validate mapping passenger -> ticket
        assertThat(inserted)
                .extracting(TicketRepository.TicketRow::passengerId, TicketRepository.TicketRow::ticketNumber,
                        TicketRepository.TicketRow::bookingId, TicketRepository.TicketRow::status)
                .containsExactlyInAnyOrder(
                        tuple(p1, "1760000000001", bookingId, "ISSUED"),
                        tuple(p2, "1760000000002", bookingId, "ISSUED")
                );

        // ticketingService called once per passenger
        verify(ticketingService, times(2)).generateTicketNumber();
    }

    @Test
    void bookingFinalize_shouldThrow_whenStatusUpdateNotOne_andNotInsertTickets() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        var cmd = ConfirmBookingCommand.builder().bookingId(bookingId).build();

        UUID p1 = UUID.randomUUID();

        var snap = snapshot(
                bookingId, "BR-DRAFT", flightId,
                "DRAFT",
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),
                List.of(seatLine(p1, "12A", "ECONOMY")),
                List.of(passengerLine(p1, "John", "Doe"))
        );

        when(bookingQueryRepository.getSnapshot(bookingId)).thenReturn(snap);

        doNothing().when(seatInventoryService).confirmLockedSeatsOrThrow(flightId, bookingId, List.of("12A"));

        when(bookingRepository.updateStatus(eq(bookingId), eq("DRAFT"), eq("CONFIRMED"), any(OffsetDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> handler.bookingFinalize(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("concurrently");

        verify(seatInventoryService).confirmLockedSeatsOrThrow(flightId, bookingId, List.of("12A"));
        verify(bookingRepository).updateStatus(eq(bookingId), eq("DRAFT"), eq("CONFIRMED"), any(OffsetDateTime.class));

        verifyNoInteractions(ticketRepository);
        verifyNoInteractions(ticketingService);
    }

    // ---------------- helpers ----------------

    private static BookingQueryRepository.BookingSnapshot snapshot(
            UUID bookingId,
            String bookingRef,
            UUID flightId,
            String status,
            OffsetDateTime holdExpiresAt,
            List<BookingQueryRepository.BookingSnapshot.SeatLine> seats,
            List<BookingQueryRepository.BookingSnapshot.PassengerLine> passengers
    ) {
        return new BookingQueryRepository.BookingSnapshot(
                bookingId,
                bookingRef,
                flightId,
                status,
                holdExpiresAt,
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

    // AssertJ tuples helper import
    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}
