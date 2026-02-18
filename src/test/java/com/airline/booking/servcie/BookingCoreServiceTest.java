package com.airline.booking.servcie;

import com.airline.booking.application.command.dto.InitiateBookingSeatCommand;
import com.airline.booking.domain.model.*;
import com.airline.booking.exception.BookingHoldExpiredException;
import com.airline.booking.exception.IllegaBookingStatus;
import com.airline.booking.service.core.BookingCoreService;
import com.airline.booking.service.core.TicketingCoreService;
import com.airline.flightmgmt.domain.FareClass;
import com.airline.shared.exception.ErrorNotification;
import com.airline.shared.exception.StructuralException;
import com.airline.shared.exception.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCoreServiceTest {

    @Mock
    private TicketingCoreService ticketingService;

    @InjectMocks
    private BookingCoreService service;


    @Test
    void createDraft_validCommand_shouldReturnFullyPopulatedBookingWithNormalizedData() {
        // Given
        InitiateBookingSeatCommand cmd = createValidCommand();

        // When
        Booking booking = service.createDraft(cmd);

        // Then
        assertNotNull(booking.getId());
        assertNotNull(booking.getBookingReference());
        assertEquals(8, booking.getBookingReference().length());

        assertEquals(cmd.getFlightId(), booking.getFlightId());
        assertEquals(cmd.getCustomerId(), booking.getCustomerId());
        assertEquals("INR", booking.getCurrency());                    // trimmed + uppercased
        assertEquals(BookingStatus.DRAFT, booking.getStatus());
        assertNotNull(booking.getBookingDate());
        assertNull(booking.getHoldExpiresAt());

        assertEquals(2, booking.getPassengers().size());
        assertEquals(2, booking.getSeats().size());
        assertTrue(booking.getTickets().isEmpty());

        // Passenger mapping + trimming
        Passenger p1 = booking.getPassengers().get(0);
        assertNotNull(p1.getId());
        assertEquals(booking.getId(), p1.getBookingId());
        assertEquals("John", p1.getFirstName());      // trimmed
        assertEquals("Doe", p1.getLastName());        // trimmed
        assertEquals(PassengerType.ADULT, p1.getPassengerType());

        Passenger p2 = booking.getPassengers().get(1);
        assertEquals("Jane", p2.getFirstName());
        assertEquals(PassengerType.CHILD, p2.getPassengerType());

        // Seat mapping + normalization + linking + new FareClass enum
        BookingSeat s1 = booking.getSeats().get(0);
        assertEquals(booking.getId(), s1.getBookingId());
        assertEquals(p1.getId(), s1.getPassengerId());
        assertEquals("12A", s1.getSeatNumber());                    // normalized upper
        assertEquals(BigDecimal.valueOf(8500), s1.getPrice());

        BookingSeat s2 = booking.getSeats().get(1);
        assertEquals(p2.getId(), s2.getPassengerId());
        assertEquals("14B", s2.getSeatNumber());

        // Total amount
        assertEquals(BigDecimal.valueOf(11500), booking.getTotalAmount());
    }

    @Test
    void createDraft_nullCommand_throwsStructuralException() {
        StructuralException ex = assertThrows(StructuralException.class,
                () -> service.createDraft(null));
        assertNotNull(ex);
    }

    @Test
    void createDraft_invalidCommand_throwsStructuralException() {
        InitiateBookingSeatCommand cmd = InitiateBookingSeatCommand.builder()
                .flightId(null)
                .customerId(null)
                .currency("   ")
                .passengers(List.of())
                .seatSelections(List.of())
                .build();

        StructuralException ex = assertThrows(StructuralException.class,
                () -> service.createDraft(cmd));

        assertNotNull(ex);
    }

    // ==================== validate (public) ====================

    @Test
    void validate_validCommand_returnsEmptyNotification() {
        ErrorNotification notification = service.validate(createValidCommand());
        assertFalse(notification.hasErrors());
        assertTrue(notification.view().isEmpty());
    }

    @Test
    void validate_nullCommand_returnsCommandRequiredError() {
        ErrorNotification n = service.validate(null);
        assertTrue(n.hasErrors());
        assertEquals(1, n.view().size());
        ValidationError err = n.view().get(0);
        assertEquals("command_required", err.code());
    }

    @Test
    void validate_missingRequiredFields_returnsAllRequiredErrors() {
        InitiateBookingSeatCommand cmd = InitiateBookingSeatCommand.builder().build();

        ErrorNotification n = service.validate(cmd);

        assertTrue(n.hasErrors());
        List<ValidationError> errors = n.view();
        assertTrue(errors.stream().anyMatch(e -> "flight_id_required".equals(e.code())));
        assertTrue(errors.stream().anyMatch(e -> "customer_id_required".equals(e.code())));
        assertTrue(errors.stream().anyMatch(e -> "currency_required".equals(e.code())));
        assertTrue(errors.stream().anyMatch(e -> "passengers_required".equals(e.code())));
        assertTrue(errors.stream().anyMatch(e -> "seat_selections_required".equals(e.code())));
    }

    @Test
    void validate_passengerSeatCountMismatch_returnsMismatchError() {
        InitiateBookingSeatCommand cmd = createValidCommand();
        cmd.setSeatSelections(List.of(cmd.getSeatSelections().get(0))); // only 1 seat for 2 passengers

        ErrorNotification n = service.validate(cmd);
        assertTrue(n.hasErrors());
        assertTrue(n.view().stream().anyMatch(e -> "seat_selections_mismatch".equals(e.code())));
    }

    @Test
    void validate_invalidPassengerIndex_returnsError() {
        InitiateBookingSeatCommand cmd = createValidCommand();
        cmd.getSeatSelections().get(0).setPassengerIndex(99);

        ErrorNotification n = service.validate(cmd);
        assertTrue(n.hasErrors());
        assertTrue(n.view().stream().anyMatch(e -> "invalid_passenger_index".equals(e.code())));
    }

    @Test
    void validate_duplicateSeat_returnsDuplicateError() {
        InitiateBookingSeatCommand cmd = createValidCommand();
        cmd.getSeatSelections().get(1).setSeatNumber("12A");

        ErrorNotification n = service.validate(cmd);
        assertTrue(n.hasErrors());
        assertTrue(n.view().stream().anyMatch(e -> "duplicate_seat".equals(e.code())));
    }

    @Test
    void validate_negativePrice_returnsInvalidPriceError() {
        InitiateBookingSeatCommand cmd = createValidCommand();
        cmd.getSeatSelections().get(0).setPrice(BigDecimal.valueOf(-100));

        ErrorNotification n = service.validate(cmd);
        assertTrue(n.hasErrors());
        assertTrue(n.view().stream().anyMatch(e -> "invalid_seat_price".equals(e.code())));
    }

    // ==================== cancel ====================

    @Test
    void cancel_draftBooking_setsStatusToCancelled() {
        Booking booking = Booking.builder().status(BookingStatus.DRAFT).build();
        service.cancel(booking);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void cancel_confirmedBooking_setsCancelledAndCancelsIssuedTickets() {
        Ticket issuedTicket = Ticket.builder().status(TicketStatus.ISSUED).build();
        Ticket alreadyCancelled = Ticket.builder().status(TicketStatus.CANCELLED).build();

        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .tickets(List.of(issuedTicket, alreadyCancelled))
                .build();

        service.cancel(booking);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(TicketStatus.CANCELLED, issuedTicket.getStatus());
        assertEquals(TicketStatus.CANCELLED, alreadyCancelled.getStatus());
    }

    @Test
    void cancel_alreadyCancelledOrExpired_isIdempotent() {
        Booking cancelled = Booking.builder().status(BookingStatus.CANCELLED).build();
        Booking expired = Booking.builder().status(BookingStatus.EXPIRED).build();

        service.cancel(cancelled);
        service.cancel(expired);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BookingStatus.EXPIRED, expired.getStatus());
    }

    @Test
    void cancel_invalidStatus_throwsIllegalStateException() {
        Booking checkedIn = Booking.builder().status(BookingStatus.CHECKED_IN).build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.cancel(checkedIn));

        assertTrue(ex.getMessage().contains("Cannot cancel in status"));
    }

    // ==================== confirm ====================

    @Test
    void confirm_draftBookingWithValidHold_setsConfirmedAndIssuesTickets() {
        when(ticketingService.generateTicketNumber()).thenReturn("TKT-12345");

        Passenger p1 = Passenger.builder().id(UUID.randomUUID()).build();
        Passenger p2 = Passenger.builder().id(UUID.randomUUID()).build();

        Booking booking = Booking.builder()
                .status(BookingStatus.DRAFT)
                .holdExpiresAt(OffsetDateTime.now().plusMinutes(30))
                .passengers(List.of(p1, p2))
                .tickets(new java.util.ArrayList<>())
                .build();

        service.confirm(booking);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(2, booking.getTickets().size());
        verify(ticketingService, times(2)).generateTicketNumber();
    }

    @Test
    void confirm_alreadyConfirmed_isIdempotentAndDoesNotCallTicketing() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .tickets(List.of(Ticket.builder().status(TicketStatus.ISSUED).build()))
                .build();

        service.confirm(booking);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(ticketingService, never()).generateTicketNumber();
    }

    @Test
    void confirm_nonDraftStatus_throwsIllegaBookingStatus() {
        Booking booking = Booking.builder().status(BookingStatus.CANCELLED).build();

        IllegaBookingStatus ex = assertThrows(IllegaBookingStatus.class,
                () -> service.confirm(booking));

        assertTrue(ex.getMessage().contains("Cannot confirm booking in status"));
    }

    @Test
    void confirm_holdExpired_throwsBookingHoldExpiredException() {
        Booking booking = Booking.builder()
                .status(BookingStatus.DRAFT)
                .holdExpiresAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        assertThrows(BookingHoldExpiredException.class, () -> service.confirm(booking));
    }


    private InitiateBookingSeatCommand createValidCommand() {
        InitiateBookingSeatCommand.Passenger pax1 = InitiateBookingSeatCommand.Passenger.builder()
                .firstName(" John ")
                .lastName(" Doe ")
                .passengerType("adult")
                .email("john@example.com")
                .phone("+911234567890")
                .build();

        InitiateBookingSeatCommand.Passenger pax2 = InitiateBookingSeatCommand.Passenger.builder()
                .firstName("Jane")
                .lastName("Smith")
                .passengerType("CHILD")
                .email("jane@example.com")
                .phone("+919876543210")
                .build();

        InitiateBookingSeatCommand.SeatSelection seat1 = InitiateBookingSeatCommand.SeatSelection.builder()
                .passengerIndex(0)
                .seatNumber("12a")
                .fareClass("ECONOMY")           // Updated to match new enum
                .price(BigDecimal.valueOf(8500))
                .build();

        InitiateBookingSeatCommand.SeatSelection seat2 = InitiateBookingSeatCommand.SeatSelection.builder()
                .passengerIndex(1)
                .seatNumber("14b")
                .fareClass("BUSINESS")          // Updated to match new enum
                .price(BigDecimal.valueOf(3000))
                .build();

        return InitiateBookingSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .currency("Inr")
                .passengers(List.of(pax1, pax2))
                .seatSelections(List.of(seat1, seat2))
                .build();
    }
}