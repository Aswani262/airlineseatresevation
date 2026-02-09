package com.airline.booking.api;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.BookSeatUseCase;
import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelBookingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BookSeatUseCase bookSeatUseCase;
    @MockBean CancelBookingUseCase cancelBookingUseCase;

    @Test
    void book_shouldReturn201_andDelegateToUseCase() throws Exception {
        UUID bookingId = UUID.randomUUID();
        OffsetDateTime hold = OffsetDateTime.of(2026, 2, 8, 10, 0, 0, 0, ZoneOffset.UTC);

        BookSeatResult result = new BookSeatResult(bookingId, "ABC12345", "DRAFT", hold);

        when(bookSeatUseCase.book(any(BookSeatCommand.class))).thenReturn(result);

        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        // minimal valid payload (based on your DTO fields)
        String json = """
            {
              "flightId": "%s",
              "customerId": "%s",
              "currency": "inr",
              "passengers": [
                {
                  "firstName": "John",
                  "lastName": "Doe",
                  "passengerType": "ADULT",
                  "email": "john@example.com",
                  "phone": "999",
                  "passportNumber": "P1"
                }
              ],
              "seatSelections": [
                {
                  "passengerIndex": 0,
                  "seatNumber": "12A",
                  "fareClass": "ECONOMY",
                  "price": 500.00
                }
              ]
            }
        """.formatted(flightId, customerId);

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.bookingReference").value("ABC12345"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
               // .andExpect(jsonPath("$.expiresAt").value(hold.toString()));

        ArgumentCaptor<BookSeatCommand> captor = ArgumentCaptor.forClass(BookSeatCommand.class);
        verify(bookSeatUseCase).book(captor.capture());

        BookSeatCommand cmd = captor.getValue();
        assertThat(cmd.getFlightId()).isEqualTo(flightId);
        assertThat(cmd.getCustomerId()).isEqualTo(customerId);
        assertThat(cmd.getCurrency()).isEqualTo("inr"); // controller doesn't normalize
        assertThat(cmd.getPassengers()).hasSize(1);
        assertThat(cmd.getPassengers().get(0).getFirstName()).isEqualTo("John");
        assertThat(cmd.getPassengers().get(0).getLastName()).isEqualTo("Doe");
        assertThat(cmd.getPassengers().get(0).getPassengerType()).isEqualTo("ADULT");
        assertThat(cmd.getPassengers().get(0).getEmail()).isEqualTo("john@example.com");
        assertThat(cmd.getPassengers().get(0).getPhone()).isEqualTo("999");
        assertThat(cmd.getPassengers().get(0).getPassportNumber()).isEqualTo("P1");
        assertThat(cmd.getSeatSelections()).hasSize(1);
        assertThat(cmd.getSeatSelections().get(0).getPassengerIndex()).isEqualTo(0);
        assertThat(cmd.getSeatSelections().get(0).getSeatNumber()).isEqualTo("12A");
        assertThat(cmd.getSeatSelections().get(0).getFareClass()).isEqualTo("ECONOMY");
        assertThat(cmd.getSeatSelections().get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(500.00));

        verifyNoMoreInteractions(bookSeatUseCase);
        verifyNoInteractions(cancelBookingUseCase);
    }

    @Test
    void cancel_shouldOverrideBookingIdFromPath_andReturn200() throws Exception {
        UUID pathBookingId = UUID.randomUUID();

        CancelBookingResult res = new CancelBookingResult(pathBookingId, "BR-001", "CANCELLED");
        when(cancelBookingUseCase.cancel(any(CancelBookingCommand.class))).thenReturn(res);

        // body bookingId is intentionally different to verify override
        UUID bodyBookingId = UUID.randomUUID();

        String json = """
            {
              "bookingId": "%s",
              "reason": "changed mind"
            }
        """.formatted(bodyBookingId);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", pathBookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingId").value(pathBookingId.toString()))
                .andExpect(jsonPath("$.bookingReference").value("BR-001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());

        CancelBookingCommand cmd = captor.getValue();
        assertThat(cmd.getBookingId()).isEqualTo(pathBookingId); // URL authoritative
        assertThat(cmd.getReason()).isEqualTo("changed mind");

        verifyNoMoreInteractions(cancelBookingUseCase);
        verifyNoInteractions(bookSeatUseCase);
    }

}