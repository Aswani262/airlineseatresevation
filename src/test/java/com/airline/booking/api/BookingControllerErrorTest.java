package com.airline.booking.api;

import com.airline.booking.application.command.BookSeatUseCase;
import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.shared.exception.ApiErrorResponse;
import com.airline.shared.exception.ApiExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(ApiExceptionHandler.class)   // 👈 important
class BookingControllerErrorTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    BookSeatUseCase bookSeatUseCase;

    @MockBean
    CancelBookingUseCase cancelBookingUseCase;

    @Test
    void book_shouldReturn400_whenUseCaseThrowsIllegalArgumentException() throws Exception {
        when(bookSeatUseCase.book(any()))
                .thenThrow(new IllegalArgumentException("flightId is required"));

        // Missing flightId in request → BookingCoreService.validate() throws IllegalArgumentException
        String json = """
            {
              "customerId": "11111111-1111-1111-1111-111111111111",
              "currency": "INR",
              "passengers": [
                {"firstName":"A","lastName":"B","passengerType":"ADULT"}
              ],
              "seatSelections": [
                {"passengerIndex":0,"seatNumber":"12A","fareClass":"ECONOMY","price":100}
              ]
            }
        """;

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("flightId is required"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(bookSeatUseCase).book(any());
        verifyNoInteractions(cancelBookingUseCase);
    }

    @Test
    void book_shouldReturn409_whenUseCaseThrowsIllegalStateException() throws Exception {
        when(bookSeatUseCase.book(any()))
                .thenThrow(new IllegalStateException("One or more seats are not available"));

        String json = """
            {
              "flightId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "customerId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "currency": "INR",
              "passengers": [
                {"firstName":"A","lastName":"B","passengerType":"ADULT"}
              ],
              "seatSelections": [
                {"passengerIndex":0,"seatNumber":"12A","fareClass":"ECONOMY","price":100}
              ]
            }
        """;

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("One or more seats are not available"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(bookSeatUseCase).book(any());
        verifyNoInteractions(cancelBookingUseCase);
    }

    @Test
    void cancel_shouldReturn400_whenUseCaseThrowsIllegalArgumentException() throws Exception {
        when(cancelBookingUseCase.cancel(any()))
                .thenThrow(new IllegalArgumentException("reason is required"));

        String json = "{}";

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("reason is required"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(cancelBookingUseCase).cancel(any());
        verifyNoInteractions(bookSeatUseCase);
    }
}