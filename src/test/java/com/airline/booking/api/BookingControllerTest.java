package com.airline.booking.api;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.BookSeatUseCase;
import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.booking.application.command.dto.InitiateBookingSeatCommand;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @InjectMocks
    private BookingController bookingController;

    @Mock
    private BookSeatUseCase bookSeatUseCase;

    @Mock
    private CancelBookingUseCase cancelBookingUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void book_validCommand_returnsCreatedWithResult() throws Exception {
        // Arrange
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        InitiateBookingSeatCommand command = InitiateBookingSeatCommand.builder()
                .flightId(flightId)
                .customerId(customerId)
                .currency("USD")
                .passengers(List.of(
                        InitiateBookingSeatCommand.Passenger.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .passengerType("ADULT")
                                .email("john@example.com")
                                .phone("1234567890")
                                .passportNumber("ABC123")
                                .build()
                ))
                .seatSelections(List.of(
                        InitiateBookingSeatCommand.SeatSelection.builder()
                                .passengerIndex(0)
                                .seatNumber("A1")
                                .fareClass("ECONOMY")
                                .price(BigDecimal.valueOf(100.00))
                                .build()
                ))
                .build();

        BookSeatResult result = new BookSeatResult(UUID.randomUUID(), "REF123", "DRAFT", OffsetDateTime.now().plusMinutes(10));

        when(bookSeatUseCase.initiateBooking(any(InitiateBookingSeatCommand.class))).thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(result.bookingId().toString()))
                .andExpect(jsonPath("$.bookingReference").value(result.bookingReference()))
                .andExpect(jsonPath("$.status").value(result.status()));

        ArgumentCaptor<InitiateBookingSeatCommand> captor = ArgumentCaptor.forClass(InitiateBookingSeatCommand.class);
        verify(bookSeatUseCase).initiateBooking(captor.capture());
        InitiateBookingSeatCommand captured = captor.getValue();
        assertEquals(flightId, captured.getFlightId());
        assertEquals(customerId, captured.getCustomerId());
        assertEquals("USD", captured.getCurrency());
        assertEquals(1, captured.getPassengers().size());
        assertEquals("John", captured.getPassengers().get(0).getFirstName());
        assertEquals(1, captured.getSeatSelections().size());
        assertEquals("A1", captured.getSeatSelections().get(0).getSeatNumber());
    }

    @Test
    void cancel_validCommand_returnsOkWithResult() throws Exception {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        CancelBookingCommand command = CancelBookingCommand.builder()
                .reason("Test cancel")
                .build();

        CancelBookingResult result = new CancelBookingResult(bookingId, "REF123", "CANCELLED");

        when(cancelBookingUseCase.cancel(any(CancelBookingCommand.class))).thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.bookingReference").value(result.getBookingReference()))
                .andExpect(jsonPath("$.status").value(result.getStatus()));

        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());
        CancelBookingCommand captured = captor.getValue();
        assertEquals(bookingId, captured.getBookingId());
        assertEquals("Test cancel", captured.getReason());
    }
}