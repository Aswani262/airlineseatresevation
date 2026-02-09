package com.airline.booking.servcie;

import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.domain.model.FareClassCode;
import com.airline.booking.domain.model.Passenger;
import com.airline.booking.domain.model.PassengerType;
import com.airline.booking.service.BookingCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class BookingCoreServiceTest {

    private BookingCoreService service;

    @BeforeEach
    void setup() {
        service = new BookingCoreService();
    }

    @Test
    void createDraft_shouldBuildValidBooking_whenCommandIsValid() {
        // Given
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        BookSeatCommand.Passenger p1 = BookSeatCommand.Passenger.builder()
                .firstName("  John  ")
                .lastName("  Doe  ")
                .email("john@example.com")
                .phone("1234567890")
                .passportNumber("P12345")
                .passengerType(" adult ")
                .build();

        BookSeatCommand.Passenger p2 = BookSeatCommand.Passenger.builder()
                .firstName("Jane")
                .lastName("Roe")
                .passengerType("Child")
                .build();  // email, phone, passport null

        BookSeatCommand.SeatSelection s1 = BookSeatCommand.SeatSelection.builder()
                .passengerIndex(0)
                .seatNumber(" 12a ")
                .fareClass(" economy ")
                .price(new BigDecimal("100.00"))
                .build();

        BookSeatCommand.SeatSelection s2 = BookSeatCommand.SeatSelection.builder()
                .passengerIndex(1)
                .seatNumber("14B")
                .fareClass("Business")
                .price(new BigDecimal("200.00"))
                .build();

        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(flightId)
                .customerId(customerId)
                .currency("  inr  ")
                .passengers(List.of(p1, p2))
                .seatSelections(List.of(s1, s2))
                .build();

        // When
        Booking booking = service.createDraft(cmd);

        // Then
        assertThat(booking).isNotNull();
        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getBookingReference()).hasSize(8)
                .matches("[A-Z0-9]+");  // From ALPHANUM, uppercase letters and digits 2-9 excluding 0,1,I,O

        assertThat(booking.getFlightId()).isEqualTo(flightId);
        assertThat(booking.getCustomerId()).isEqualTo(customerId);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(booking.getCurrency()).isEqualTo("INR");  // normalized
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.DRAFT);
        assertThat(booking.getBookingDate()).isNotNull();  // approximately now
        assertThat(booking.getHoldExpiresAt()).isNull();
        assertThat(booking.getTickets()).isEmpty();

        // Passengers
        List<Passenger> passengers = booking.getPassengers();
        assertThat(passengers).hasSize(2);

        Passenger passenger1 = passengers.get(0);
        assertThat(passenger1.getId()).isNotNull();
        assertThat(passenger1.getBookingId()).isEqualTo(booking.getId());
        assertThat(passenger1.getFirstName()).isEqualTo("John");  // trimmed
        assertThat(passenger1.getLastName()).isEqualTo("Doe");    // trimmed
        assertThat(passenger1.getEmail()).isEqualTo("john@example.com");
        assertThat(passenger1.getPhone()).isEqualTo("1234567890");
        assertThat(passenger1.getPassportNumber()).isEqualTo("P12345");
        assertThat(passenger1.getDateOfBirth()).isNull();
        assertThat(passenger1.getPassengerType()).isEqualTo(PassengerType.ADULT);  // normalized

        Passenger passenger2 = passengers.get(1);
        assertThat(passenger2.getId()).isNotNull();
        assertThat(passenger2.getBookingId()).isEqualTo(booking.getId());
        assertThat(passenger2.getFirstName()).isEqualTo("Jane");
        assertThat(passenger2.getLastName()).isEqualTo("Roe");
        assertThat(passenger2.getEmail()).isNull();
        assertThat(passenger2.getPhone()).isNull();
        assertThat(passenger2.getPassportNumber()).isNull();
        assertThat(passenger2.getDateOfBirth()).isNull();
        assertThat(passenger2.getPassengerType()).isEqualTo(PassengerType.CHILD);  // normalized

        // Seats
        List<BookingSeat> seats = booking.getSeats();
        assertThat(seats).hasSize(2);

        BookingSeat seat1 = seats.get(0);
        assertThat(seat1.getId()).isNotNull();
        assertThat(seat1.getBookingId()).isEqualTo(booking.getId());
        assertThat(seat1.getPassengerId()).isEqualTo(passenger1.getId());
        assertThat(seat1.getSeatNumber()).isEqualTo("12A");  // normalized
        assertThat(seat1.getFareClass()).isEqualTo(new FareClassCode("ECONOMY"));  // normalized
        assertThat(seat1.getPrice()).isEqualByComparingTo("100.00");

        BookingSeat seat2 = seats.get(1);
        assertThat(seat2.getId()).isNotNull();
        assertThat(seat2.getBookingId()).isEqualTo(booking.getId());
        assertThat(seat2.getPassengerId()).isEqualTo(passenger2.getId());
        assertThat(seat2.getSeatNumber()).isEqualTo("14B");
        assertThat(seat2.getFareClass()).isEqualTo(new FareClassCode("BUSINESS"));  // normalized
        assertThat(seat2.getPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenCommandIsNull() {
        assertThatThrownBy(() -> service.createDraft(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("command is required");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenFlightIdIsNull() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .customerId(UUID.randomUUID())
                .currency("INR")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder().passengerIndex(0).seatNumber("12A").fareClass("ECONOMY").price(BigDecimal.TEN).build()))
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("flightId is required");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenCustomerIdIsNull() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .currency("INR")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder().passengerIndex(0).seatNumber("12A").fareClass("ECONOMY").price(BigDecimal.TEN).build()))
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerId is required");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenCurrencyIsBlank() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .currency("   ")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder().passengerIndex(0).seatNumber("12A").fareClass("ECONOMY").price(BigDecimal.TEN).build()))
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currency is required");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenPassengersIsEmpty() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .currency("INR")
                .passengers(List.of())
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder().passengerIndex(0).seatNumber("12A").fareClass("ECONOMY").price(BigDecimal.TEN).build()))
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passengers is required");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenSeatSelectionsIsEmpty() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .currency("INR")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of())
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("seatSelections is required");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenPassengerIndexInvalid() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .currency("INR")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder().passengerIndex(1).seatNumber("12A").fareClass("ECONOMY").price(BigDecimal.TEN).build()))  // index 1 out of bounds
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid passengerIndex: 1");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenSeatNumberBlank() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .currency("INR")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder().passengerIndex(0).seatNumber("   ").fareClass("ECONOMY").price(BigDecimal.TEN).build()))
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("seatNumber is required");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenPriceNegative() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .currency("INR")
                .passengers(List.of(BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build()))
                .seatSelections(List.of(BookSeatCommand.SeatSelection.builder().passengerIndex(0).seatNumber("12A").fareClass("ECONOMY").price(new BigDecimal("-10")).build()))
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid seat price");
    }

    @Test
    void createDraft_shouldThrowIllegalArgumentException_whenDuplicateSeats() {
        BookSeatCommand cmd = BookSeatCommand.builder()
                .flightId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .currency("INR")
                .passengers(List.of(
                        BookSeatCommand.Passenger.builder().firstName("A").lastName("B").passengerType("ADULT").build(),
                        BookSeatCommand.Passenger.builder().firstName("C").lastName("D").passengerType("ADULT").build()
                ))
                .seatSelections(List.of(
                        BookSeatCommand.SeatSelection.builder().passengerIndex(0).seatNumber("12a").fareClass("ECONOMY").price(BigDecimal.TEN).build(),
                        BookSeatCommand.SeatSelection.builder().passengerIndex(1).seatNumber(" 12A ").fareClass("ECONOMY").price(BigDecimal.TEN).build()  // duplicate after normalize
                ))
                .build();

        assertThatThrownBy(() -> service.createDraft(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate seat in request: 12A");
    }
}